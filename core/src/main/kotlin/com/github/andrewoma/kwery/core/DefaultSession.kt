/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.kwery.core

import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import java.sql.Statement
import java.sql.PreparedStatement
import java.sql.SQLException
import com.github.andrewoma.kwery.core.interceptor.StatementInterceptor
import com.github.andrewoma.kwery.core.dialect.Dialect
import java.util.ArrayList
import com.github.andrewoma.kommon.lang.trimMargin
import com.github.andrewoma.kwery.core.interceptor.noOpStatementInterceptor

/**
 * DefaultSession is NOT thread safe. It seems the underlying JDBC drivers are patchy with thread safety
 * on a single connection, so it seems a little pointless to make this safe.
 *
 * Typically, use a ThreadLocalSession in server environments. Alternatively, use a connection pool
 * and create a new session per thread.
 */
public class DefaultSession(override val connection: Connection,
                            override val dialect: Dialect,
                            val interceptor: StatementInterceptor = noOpStatementInterceptor,
                            override val defaultSelectOptions: SelectOptions = SelectOptions(),
                            override val defaultUpdateOptions: UpdateOptions = UpdateOptions()) : Session {

    class object {
        private val namedQueryCache = ConcurrentHashMap<StatementCacheKey, BoundQuery>()
    }

    override public val currentTransaction: Transaction?
        get() = transaction

    var transaction: Transaction? = null

    override public fun <R> select(sql: String, parameters: Map<String, Any?>, options: SelectOptions, mapper: (Row) -> R): List<R> {
        return withPreparedStatement(sql, listOf(parameters), options) {(statement, ps) ->
            bindParameters(parameters, statement)

            val result = arrayListOf<R>()
            val rs = ps.executeQuery()
            var count = 0
            interceptor.executed(statement)
            while (rs.next()) {
                result.add(mapper(Row(rs)))
                count++
            }
            rs.close()
            statement.copy(rowsCounts = listOf(count)) to result
        }
    }

    override fun batchUpdate(sql: String, parametersList: List<Map<String, Any?>>, options: UpdateOptions): List<Int> {
        require(!parametersList.isEmpty(), "Parameters cannot be empty for batchUpdate")

        return withPreparedStatement(sql, parametersList, options) {(statement, ps) ->
            for (parameters in parametersList) {
                bindParameters(parameters, statement)
                ps.addBatch()
            }
            val rowsAffected = ps.executeBatch().toList()
            interceptor.executed(statement)
            statement.copy(rowsCounts = rowsAffected) to rowsAffected
        }
    }

    override fun <K> batchUpdate(sql: String, parametersList: List<Map<String, Any?>>, options: UpdateOptions, f: (Row) -> K): List<Pair<Int, K>> {
        require(!parametersList.isEmpty(), "Parameters cannot be empty for batchUpdate")

        return withPreparedStatement(sql, parametersList, options.copy(useGeneratedKeys = true)) {(statement, ps) ->
            for (parameters in parametersList) {
                bindParameters(parameters, statement)
                ps.addBatch()
            }
            val rowsAffected = ps.executeBatch().toList()
            interceptor.executed(statement)

            val rs = ps.getGeneratedKeys()
            val keys = ArrayList<K>(parametersList.size())
            while (rs.next()) {
                keys.add(f(Row(rs)))
            }
            require(keys.size() == parametersList.size()) { "Expected ${parametersList.size()} keys but received ${keys.size()}" }
            statement.copy(rowsCounts = rowsAffected) to rowsAffected.zip(keys)
        }
    }

    override public fun update(sql: String, parameters: Map<String, Any?>, options: UpdateOptions): Int {
        return withPreparedStatement(sql, listOf(parameters), options) {(statement, ps) ->
            bindParameters(parameters, statement)
            val rowsAffected = ps.executeUpdate()
            interceptor.executed(statement)
            statement.copy(rowsCounts = listOf(rowsAffected)) to rowsAffected
        }
    }

    override public fun <K> update(sql: String, parameters: Map<String, Any?>, options: UpdateOptions, f: (Row) -> K): Pair<Int, K> {
        return withPreparedStatement(sql, listOf(parameters), options.copy(useGeneratedKeys = true)) {(statement, ps) ->
            bindParameters(parameters, statement)
            val rowsAffected = ps.executeUpdate()
            interceptor.executed(statement)
            val rs = ps.getGeneratedKeys()
            require(rs.next(), "No generated key received")
            val keys = f(Row(rs))
            statement.copy(rowsCounts = listOf(rowsAffected)) to (rowsAffected to keys)
        }
    }

    override public fun stream(sql: String, parameters: Map<String, Any?>, options: SelectOptions, f: (Row) -> Unit): Unit {
        withPreparedStatement(sql, listOf(parameters), options) {(statement, ps) ->
            bindParameters(parameters, statement)
            val rs = ps.executeQuery()
            interceptor.executed(statement)
            var count = 0
            while (rs.next()) {
                count++
                f(Row(rs))
            }
            rs.close()
            statement.copy(rowsCounts = listOf(count)) to 1
        }
    }

    override public fun bindParameters(sql: String, parameters: Map<String, Any?>): String {
        return replaceBindings(sql) { key ->
            val value = parameters[key]
            when (value) {
                null -> "null"
                is Collection<*> -> {
                    value.map { if (it == null) "null" else dialect.bind(it) }.joinToString(",")
                }
                else -> dialect.bind(value)
            }
        }
    }

    override public fun <R> transaction(f: (Transaction) -> R): R {
        return DefaultTransaction(this).withTransaction(f)
    }

    override public fun manualTransaction(): ManualTransaction {
        return DefaultTransaction(this)
    }

    private fun <R> withPreparedStatement(sql: String, parameters: List<Map<String, Any?>> = listOf(), options: StatementOptions,
                                          f: (ExecutingStatement, PreparedStatement) -> Pair<ExecutingStatement, R>): R {
        var statement = ExecutingStatement(this, hashMapOf(), sql, parameters, options)
        try {
            statement = interceptor.construct(statement)

            statement = statement.copy(inClauseSizes = inClauseSizes(parameters))
            val namedQuery = namedQueryCache.getOrPut(StatementCacheKey(sql, statement.inClauseSizes, options.cacheKey)) {
                statement = statement.copy(sql = sql.trimMargin())
                BoundQuery(statement.sql, statement.inClauseSizes)
            }
            statement = interceptor.preparing(statement.copy(preparedSql = namedQuery.query, preparedParameters = namedQuery.bindings))

            val generatedKeys = if (options is UpdateOptions && options.useGeneratedKeys)
                Statement.RETURN_GENERATED_KEYS else Statement.NO_GENERATED_KEYS

            statement = statement.copy(statement = connection.prepareStatement(statement.preparedSql, generatedKeys))
            interceptor.prepared(statement)

            val (s, result) = f(statement, statement.statement as PreparedStatement)
            statement = s
            return result
        } catch (e: SQLException) {
            interceptor.exception(statement, e)
            throw e
        } finally {
            statement.statement?.close()
            interceptor.closed(statement)
        }
    }

    private fun bindParameters(parameters: Map<String, Any?>, statement: ExecutingStatement): PreparedStatement {
        val ps = statement.statement as PreparedStatement
        for ((i, key) in statement.preparedParameters.withIndex()) {
            val value = parameters[key]
            require(value != null || parameters.containsKey(key)) { "Unknown query parameter: '$key'" }
            if (value is Collection<*>) {
                setInClause(ps, value, statement.inClauseSizes[key]!!)
            } else {
                ps.setObject(i + 1, value)
            }
        }
        return ps
    }

    private fun setInClause(ps: PreparedStatement, values: Collection<*>, size: Int) {
        for ((i, value) in values.withIndex()) {
            ps.setObject(i + 1, value)
        }
        for (i in values.size()..size - 1) {
            ps.setObject(i + 1, null)
        }
    }
}

public data class ExecutingStatement (
        val session: Session,
        val contexts: MutableMap<String, Any?>,
        val sql: String,
        val parametersList: List<Map<String, Any?>>,
        val options: StatementOptions,
        val preparedSql: String? = null,
        val preparedParameters: List<String> = arrayListOf(),
        val statement: Statement? = null,
        val inClauseSizes: Map<String, Int> = mapOf(),
        val rowsCounts: List<Int> = listOf()
)

data class StatementCacheKey(val sql: String, val collections: Map<String, Int>, val optionsKey: Any?)
