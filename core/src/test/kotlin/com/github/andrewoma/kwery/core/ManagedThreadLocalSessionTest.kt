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

import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.dialect.PostgresDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.zaxxer.hikari.pool.ProxyConnection
import com.zaxxer.hikari.pool.ProxyStatement
import org.junit.Before
import org.junit.Test
import org.postgresql.PGStatement
import java.sql.Connection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManagedThreadLocalSessionTest {
    companion object {
        var initialised = false
        val session = ManagedThreadLocalSession(postgresDataSource, PostgresDialect(), postgresLoggingInterceptor)
        val dao = SessionInfoDao(session) // Test shared dao singleton
    }

    @Before fun setUp() {
        if (!initialised) {
            initialised = true
            val sql = """
                drop table if exists sessions_test;
                create table sessions_test (
                    thread varchar(1000),
                    pool_connection numeric(20),
                    connection numeric(20)
                )
            """
            session.use { for (statement in sql.split(";".toRegex())) session.update(statement) }
        }

        session.use { session.update("delete from sessions_test") }
    }

    fun getInsertedSessions() = session.use {
        val sessions = session.select("select * from sessions_test") { row ->
            SessionInfo(row.string("thread"), row.long("pool_connection"), row.long("connection"))
        }
        println(sessions.joinToString("\n"))
        sessions
    }

    @Test(expected = IllegalStateException::class) fun `Uninitialised thread should be rejected`() {
        dao.insertCurrent()
    }

    @Test fun `An initialised session should successfully insert`() {
        ManagedThreadLocalSession.initialise()
        dao.insertCurrent()
        ManagedThreadLocalSession.finalise(commitTransaction = true)
        val sessions = getInsertedSessions()
        assertEquals(1, sessions.size)
    }

    @Test fun `An initialised session should not insert on rollback`() {
        ManagedThreadLocalSession.initialise()
        dao.insertCurrent()
        ManagedThreadLocalSession.finalise(commitTransaction = false)
        val sessions = getInsertedSessions()
        assertEquals(0, sessions.size)
    }

    @Test fun `Inserts on different threads should use different connections`() {
        val threads = 5
        val requests = 100
        postgresLoggingInterceptor.clear()

        val executor = Executors.newFixedThreadPool(threads)

        repeat(requests) {
            executor.submit {
                ManagedThreadLocalSession.initialise()
                try {
                    dao.insertCurrent()
                } finally {
                    ManagedThreadLocalSession.finalise(commitTransaction = true)
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        val sessions = getInsertedSessions()
        assertEquals(requests, sessions.size)

        // Should use a different connection per request (will usually be pooled)
        assertEquals(requests, sessions.groupBy { it.poolConnection }.size)

        // Should have executed in multiple threads
        assertEquals(threads, sessions.groupBy { it.thread }.size)

        // Should have used 1 underlying connection per thread
        assertEquals(threads, sessions.groupBy { it.connection }.size)

        // Should have a majority server side prepared (by default the Postgres driver waits
        // unit a statement is used 5 times before it prepares on the server)
        assertTrue(postgresLoggingInterceptor.serverPrepared.toDouble() / postgresLoggingInterceptor.total.toDouble() > .8)
    }

    @Test fun `Use should allocate a session automatically`() {
        var count: Int? = null
        val thread = Thread {
            count = session.use {
                session.select("select count(*) c from sessions_test") { it.int("c") }
            }.single()
        }
        thread.start()
        thread.join()
        assertNotNull(count)
    }

    @Test fun `Multiple datasources should be supported on same thread`() {
        val hsqlSession = ManagedThreadLocalSession(hsqlDataSource, HsqlDialect(), name = "hsql")
        assertEquals("HSQLDB", hsqlSession.use {
            hsqlSession.select(dbNameSql) { it.string("name") }.single()
        }.trim())

        assertEquals("PostgreSQL", session.use {
            session.select(dbNameSql) { it.string("name") }.single()
        }.trim())
    }

    @Test fun `Use should rollback on exception`() {
        try {
            session.use {
                SessionInfoDao(session).insertCurrent()
                throw Exception()
            }
        } catch(e: Exception) {
            assertEquals(0, getInsertedSessions().size)
        }
    }
}

data class SessionInfo(val thread: String, val poolConnection: Long, val connection: Long)

class SessionInfoDao(val session: Session) {
    fun insertCurrent() {
        val sql = "insert into sessions_test(thread, pool_connection, connection) values (:thread, :pool_connection, :connection)"
        val params = mapOf("thread" to Thread.currentThread().name,
                "pool_connection" to System.identityHashCode(session.connection),
                "connection" to System.identityHashCode((session.connection as ProxyConnection).unwrap(Connection::class.java)))
        session.update(sql, params)
    }
}

object postgresLoggingInterceptor : LoggingInterceptor() {
    var serverPrepared = 0
    var total = 0

    fun clear() {
        serverPrepared = 0
        total = 0
    }

    override fun additionalInfo(statement: ExecutingStatement): String {
        val st = statement.statement!!
        val pgStatement = (st as ProxyStatement).unwrap(PGStatement::class.java)

        total++

        if (pgStatement.isUseServerPrepare) {
            serverPrepared++
        }

        return ". Connection: " + System.identityHashCode((st.connection as ProxyConnection).unwrap(Connection::class.java)) +
                ". ServerPrepared=" + pgStatement.isUseServerPrepare
    }
}
