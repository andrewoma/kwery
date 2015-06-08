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
 * ThreadLocalSession creates sessions on a per thread basis. It allows services
 * using sessions to be defined with static references to sessions without worrying about
 * managing connections (or transactions).
 *
 * Typically, a single instance of ThreadLocalSession per data source would
 * be shared amongst services. In a container environment a per thread filter or similar mechanism
 * would then initialise and finalise a session before use.
 *
 * Sessions are allocated lazily (on first use of the session). So there is no overhead in terms of
 * allocating connections if the thread doesn't actually use a session.
 */
private val defaultThreadLocalSessionName = "default"

public class ThreadLocalSession(val dataSource: DataSource,
                                override val dialect: Dialect,
                                val interceptor: StatementInterceptor = noOpStatementInterceptor,
                                val name: String = defaultThreadLocalSessionName,
                                override val defaultStatementOptions: StatementOptions = StatementOptions()) : Session {

    class SessionConfig(val startTransaction: Boolean, val session: DefaultSession?, val transaction: ManualTransaction?)

    companion object {
        private val threadLocalSession = object : ThreadLocal<MutableMap<String, SessionConfig>>() {
            override fun initialValue(): MutableMap<String, SessionConfig> {
                return hashMapOf()
            }
        }

        public fun initialise(startTransaction: Boolean = true, name: String = defaultThreadLocalSessionName) {
            val configs = threadLocalSession.get()
            check(!configs.containsKey(name), "A session is already initialised for this thread")
            configs.put(name, SessionConfig(startTransaction, null, null))
        }

        public fun finalise(commitTransaction: Boolean, name: String = defaultThreadLocalSessionName) {
            val configs = threadLocalSession.get()
            val config = configs.get(name)
            check(config != null, "A session has not been initialised for this thread")

            try {
                closeSession(commitTransaction, config!!)
            } finally {
                configs.remove(name)
            }
        }

        private fun closeSession(commitTransaction: Boolean, config: SessionConfig) {
            if (config.session == null) return // A session was never created in this thread

            try {
                if (config.transaction != null) {
                    check(config.session.currentTransaction == config.transaction, "Unexpected transaction in session")
                    if (commitTransaction && !config.transaction.rollbackOnly) {
                        config.transaction.commit()
                    } else {
                        config.transaction.rollback()
                    }
                }
            } finally {
                config.session.connection.close()
            }
        }
    }

    override val currentTransaction: Transaction?
        get() = session.currentTransaction

    override val connection: Connection
        get() = session.connection

    private val session: DefaultSession
        get() {
            val configs = threadLocalSession.get()
            val config = configs.get(name) ?: error("A session has not been initialised for this thread")
            return if (config.session == null) {
                val session = DefaultSession(dataSource.getConnection(), dialect, interceptor, defaultStatementOptions)
                val transaction = if (!config.startTransaction) null else session.manualTransaction()
                configs.put(name, SessionConfig(config.startTransaction, session, transaction))
                session
            } else {
                config.session
            }
        }

    override fun <R> select(sql: String, parameters: Map<String, Any?>, options: StatementOptions, mapper: (Row) -> R): List<R> {
        return session.select(sql, parameters, options, mapper)
    }

    override fun update(sql: String, parameters: Map<String, Any?>, options: StatementOptions): Int {
        return session.update(sql, parameters, options)
    }

    override fun batchUpdate(sql: String, parametersList: List<Map<String, Any?>>, options: StatementOptions): List<Int> {
        return session.batchUpdate(sql, parametersList, options)
    }

    override fun <K> insert(sql: String, parameters: Map<String, Any?>, options: StatementOptions, f: (Row) -> K): Pair<Int, K> {
        return session.insert(sql, parameters, options, f)
    }

    override fun <K> batchInsert(sql: String, parametersList: List<Map<String, Any?>>, options: StatementOptions, f: (Row) -> K): List<Pair<Int, K>> {
        return session.batchInsert(sql, parametersList, options, f)
    }

    override fun forEach(sql: String, parameters: Map<String, Any?>, options: StatementOptions, f: (Row) -> Unit) {
        return session.forEach(sql, parameters, options, f)
    }

    override fun <R> sequence(sql: String, parameters: Map<String, Any?>, options: StatementOptions, f: (Sequence<Row>) -> R): R {
        return session.sequence(sql, parameters, options, f)
    }

    override fun bindParameters(sql: String,
                                parameters: Map<String, Any?>,
                                closeParameters: Boolean,
                                limit: Int,
                                consumeStreams: Boolean): String {
        return session.bindParameters(sql, parameters, closeParameters, limit, consumeStreams)
    }

    override fun <R> transaction(f: (Transaction) -> R): R {
        return session.transaction(f)
    }

    override fun manualTransaction(): ManualTransaction {
        return session.manualTransaction()
    }

    public fun <R> use(startTransaction: Boolean = true, f: () -> R): R {
        ThreadLocalSession.initialise(startTransaction, name)
        var commit = true
        try {
            return f()
        } catch(e: Exception) {
            commit = false
            throw e
        } finally {
            ThreadLocalSession.finalise(commit, name)
        }
    }
}
