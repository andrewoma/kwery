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

import com.github.andrewoma.kommon.lang.trimMargin
import com.github.andrewoma.kwery.core.dialect.Dialect
import com.github.andrewoma.kwery.core.interceptor.StatementInterceptor
import com.github.andrewoma.kwery.core.interceptor.noOpStatementInterceptor
import java.io.InputStream
import java.io.Reader
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kotlin.support.AbstractIterator

/**
 * DefaultSession is NOT thread safe. It seems the underlying JDBC drivers are patchy with thread safety
 * on a single connection, so it seems a little pointless to make this safe.
 *
 * Typically, use a ThreadLocalSession in server environments. Alternatively, use a connection pool
 * and create a new session per transaction using SessionFactory.
 */
public class DefaultSession(override val connection: Connection,
                            override val dialect: Dialect,
                            val interceptor: StatementInterceptor = noOpStatementInterceptor,
                            override val defaultSelectOptions: SelectOptions = SelectOptions(),
                            override val defaultUpdateOptions: UpdateOptions = UpdateOptions()) : Session {

    companion object {
        private val namedQueryCache = ConcurrentHashMap<StatementCacheKey, BoundQuery>()
    }

    override public val currentTransaction: Transaction?
        get() = transaction

    var transaction: Transaction? = null

    override public fun <R> select(sql: String, parameters: Map<String, Any?>, options: SelectOptions, mapper: (Row) -> R): List<R> {
        return withPreparedStatement(sql, listOf(parameters), options) { statement, ps ->
            bindParameters(parameters, statement)

            val result = arrayListOf<R>()
            val rs = ps.executeQuery()
            try {
                var count = 0
                interceptor.executed(statement)
                while (rs.next()) {
                    result.add(mapper(Row(rs)))
                    count++
                }
                statement.copy(rowsCounts = listOf(count)) to result
            } finally {
                rs.close()
            }
        }
    }

    override fun batchUpdate(sql: String, parametersList: List<Map<String, Any?>>, options: UpdateOptions): List<Int> {
        require(!parametersList.isEmpty(), "Parameters cannot be empty for batchUpdate")

        return withPreparedStatement(sql, parametersList, options) { statement, ps ->
            for (parameters in parametersList) {
                bindParameters(parameters, statement)
                ps.addBatch()
            }
            val rowsAffected = ps.executeBatch().toList()
            interceptor.executed(statement)
            statement.copy(rowsCounts = rowsAffected) to rowsAffected
        }
    }

    override fun <K> batchInsert(sql: String, parametersList: List<Map<String, Any?>>, options: UpdateOptions, f: (Row) -> K): List<Pair<Int, K>> {
        require(!parametersList.isEmpty(), "Parameters cannot be empty for batchUpdate")

        return withPreparedStatement(sql, parametersList, options.copy(useGeneratedKeys = true)) { statement, ps ->
            for (parameters in parametersList) {
                bindParameters(parameters, statement)
                ps.addBatch()
            }
            val rowsAffected = ps.executeBatch().toList()
            interceptor.executed(statement)

            val rs = ps.getGeneratedKeys()
            try {
                val keys = ArrayList<K>(parametersList.size())
                while (rs.next()) {
                    keys.add(f(Row(rs)))
                }
                require(keys.size() == parametersList.size()) { "Expected ${parametersList.size()} keys but received ${keys.size()}" }
                statement.copy(rowsCounts = rowsAffected) to rowsAffected.zip(keys)
            } finally {
                rs.close()
            }
        }
    }

    override public fun update(sql: String, parameters: Map<String, Any?>, options: UpdateOptions): Int {
        return withPreparedStatement(sql, listOf(parameters), options) { statement, ps ->
            bindParameters(parameters, statement)
            val rowsAffected = ps.executeUpdate()
            interceptor.executed(statement)
            statement.copy(rowsCounts = listOf(rowsAffected)) to rowsAffected
        }
    }

    override public fun <K> insert(sql: String, parameters: Map<String, Any?>, options: UpdateOptions, f: (Row) -> K): Pair<Int, K> {
        return withPreparedStatement(sql, listOf(parameters), options.copy(useGeneratedKeys = true)) { statement, ps ->
            bindParameters(parameters, statement)
            val rowsAffected = ps.executeUpdate()
            interceptor.executed(statement)
            val rs = ps.getGeneratedKeys()
            try {
                require(rs.next(), "No generated key received")
                val keys = f(Row(rs))
                statement.copy(rowsCounts = listOf(rowsAffected)) to (rowsAffected to keys)
            } finally {
                rs.close()
            }
        }
    }

    override fun <R> sequence(sql: String,
                              parameters: Map<String, Any?>,
                              options: SelectOptions,
                              f: (Sequence<Row>) -> R): R {

        return withPreparedStatement(sql, listOf(parameters), options) { statement, ps ->
            bindParameters(parameters, statement)
            val rs = ps.executeQuery()
            try {
                interceptor.executed(statement)
                val rowStream = RowStream(rs)
                val result = f(rowStream)
                statement.copy(rowsCounts = listOf(rowStream.count)) to result
            } finally {
                rs.close()
            }
        }
    }

    private class RowStream(val rs: ResultSet) : Sequence<Row> {
        var count: Int = 0

        override fun iterator(): Iterator<Row> {
            return object : AbstractIterator<Row>() {
                override fun computeNext() {
                    if (rs.next()) {
                        count++
                        setNext(Row(rs))
                    } else {
                        done()
                    }
                }
            }
        }
    }

    override public fun forEach(sql: String, parameters: Map<String, Any?>, options: SelectOptions, f: (Row) -> Unit): Unit {
        withPreparedStatement(sql, listOf(parameters), options) { statement, ps ->
            bindParameters(parameters, statement)
            val rs = ps.executeQuery()
            try {
                interceptor.executed(statement)
                var count = 0
                while (rs.next()) {
                    count++
                    f(Row(rs))
                }
                statement.copy(rowsCounts = listOf(count)) to 1
            } finally {
                rs.close()
            }
        }
    }

    override fun bindParameters(sql: String,
                                parameters: Map<String, Any?>,
                                closeParameters: Boolean,
                                limit: Int,
                                consumeStreams: Boolean): String {

        return replaceBindings(sql) { key ->
            val value = parameters[key]
            when (value) {
                null -> "null"
                is InputStream -> if (consumeStreams) dialect.bind(value, limit) else "<InputStream>"
                is Reader -> if (consumeStreams) dialect.bind(value, limit) else "<Reader>"
                is Collection<*> -> {
                    value.map { if (it == null) "null" else dialect.bind(it, limit) }.joinToString(",")
                }
                else -> dialect.bind(value, limit)
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
        } catch (e: Exception) {
            throw interceptor.exception(statement, e)
        } finally {
            try {
                interceptor.closed(statement)
            } finally {
                statement.statement?.close()
            }
        }
    }

    private fun bindParameters(parameters: Map<String, Any?>, statement: ExecutingStatement): PreparedStatement {
        val ps = statement.statement as PreparedStatement
        for ((i, key) in statement.preparedParameters.withIndex()) {
            val value = parameters[key]
            require(value != null || parameters.containsKey(key)) { "Unknown query parameter: '$key'" }
            when (value) {
                is TypedParameter -> ps.setObject(i + 1, value.value, value.sqlType)
                is InputStream -> ps.setBinaryStream(i + 1, value)
                is Reader -> ps.setCharacterStream(i + 1, value)
                is Collection<*> -> setInClause(ps, value, statement.inClauseSizes[key]!!)
                else -> ps.setObject(i + 1, value)
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

public class TypedParameter(val value: Any?, val sqlType: Int)

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
