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

import com.github.andrewoma.kwery.core.dialect.Dialect
import com.github.andrewoma.kwery.core.interceptor.StatementInterceptor
import com.github.andrewoma.kwery.core.interceptor.noOpStatementInterceptor
import java.sql.Connection
import javax.sql.DataSource

/**
 * ThreadLocalSession creates sessions on demand from a pooled DataSource.
 * By default a new Session (and therefore new connection from pooled DataSource) is created for
 * each statement. If a transaction is started, the Session is stored in a ThreadLocal for the
 * duration of the transaction and is used for any further statements during the transaction.
 */

public class ThreadLocalSession(val dataSource: DataSource,
                                override val dialect: Dialect,
                                val interceptor: StatementInterceptor = noOpStatementInterceptor,
                                val name: String = defaultThreadLocalSessionName,
                                override val defaultOptions: StatementOptions = StatementOptions()) : Session {

    companion object {
        private val threadLocalSession = ThreadLocal<Session>()
    }

    fun <R> withSession(f: (Session) -> R): R {
        val session: Session? = threadLocalSession.get()
        return if (session == null) use { f(it) } else f(session)
    }

    override val currentTransaction: Transaction?
        get() = threadLocalSession.get()?.currentTransaction ?:
                throw UnsupportedOperationException("'currentTransaction' is only supported within a transaction in ThreadLocalSession")

    override val connection: Connection
        get() = threadLocalSession.get()?.connection ?:
                throw UnsupportedOperationException("'connection' is only supported within a transaction in ThreadLocalSession")

    override fun <R> select(sql: String, parameters: Map<String, Any?>, options: StatementOptions, mapper: (Row) -> R): List<R> {
        return withSession { it.select(sql, parameters, options, mapper) }
    }

    override fun update(sql: String, parameters: Map<String, Any?>, options: StatementOptions): Int {
        return withSession { it.update(sql, parameters, options) }
    }

    override fun batchUpdate(sql: String, parametersList: List<Map<String, Any?>>, options: StatementOptions): List<Int> {
        return withSession { it.batchUpdate(sql, parametersList, options) }
    }

    override fun <K> insert(sql: String, parameters: Map<String, Any?>, options: StatementOptions, f: (Row) -> K): Pair<Int, K> {
        return withSession { it.insert(sql, parameters, options, f) }
    }

    override fun <K> batchInsert(sql: String, parametersList: List<Map<String, Any?>>, options: StatementOptions, f: (Row) -> K): List<Pair<Int, K>> {
        return withSession { it.batchInsert(sql, parametersList, options, f) }
    }

    override fun forEach(sql: String, parameters: Map<String, Any?>, options: StatementOptions, f: (Row) -> Unit) {
        return withSession { it.forEach(sql, parameters, options, f) }
    }

    override fun <R> asSequence(sql: String, parameters: Map<String, Any?>, options: StatementOptions, f: (Sequence<Row>) -> R): R {
        return withSession { it.asSequence(sql, parameters, options, f) }
    }

    override fun bindParameters(sql: String,
                                parameters: Map<String, Any?>,
                                closeParameters: Boolean,
                                limit: Int,
                                consumeStreams: Boolean): String {
        return withSession { it.bindParameters(sql, parameters, closeParameters, limit, consumeStreams) }
    }

    override fun <R> transaction(f: (Transaction) -> R): R {
        val session: Session? = threadLocalSession.get()
        return if (session == null) newTransaction(f) else f(session.currentTransaction!!)
    }

    override fun manualTransaction(): ManualTransaction {
        throw UnsupportedOperationException("'manualTransaction' is not supported in ThreadLocalSession")
    }

    private fun <R> newTransaction(f: (Transaction) -> R): R {
        return use { session ->
            threadLocalSession.set(session)
            try {
                session.transaction { f(it) }
            } finally {
                threadLocalSession.remove()
            }
        }
    }

    private fun <R> use(f: (Session) -> R): R {
        val session = DefaultSession(dataSource.connection, dialect, interceptor, defaultOptions)
        try {
            return f(session)
        } finally {
            session.connection.close()
        }
    }
}
