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

import org.junit.Test as test
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import org.junit.Before as before
import kotlin.test.assertEquals
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class SessionInfo(val thread: String, val connection: Long)

class SessionInfoDao(val session: Session) {
    fun insertCurrent() {
        val sql = "insert into sessions(thread, connection) values (:thread, :connection)"
        val params = mapOf("thread" to Thread.currentThread().getName(),
                "connection" to System.identityHashCode(session.connection))
        println("Inserting $params")
        session.update(sql, params)
    }
}

class ThreadLocalSessionTest {
    class object {
        var initialised = false
        val threadLocalSession = ThreadLocalSession(hsqlDataSource, HsqlDialect(), LoggingInterceptor())
        val dao = SessionInfoDao(threadLocalSession) // Test shared dao singleton
    }

    before fun setUp() {
        if (!initialised) {
            initialised = true
            val sql = """
                create table sessions (
                    thread varchar(1000),
                    connection numeric(20)
                )
            """
            withSession { it.update(sql) }
        }

        withSession { it.update("delete from sessions") }
    }

    fun <R> withSession(f: (Session) -> R): R {
        val connection = hsqlDataSource.getConnection()
        try {
            val defaultSession = DefaultSession(connection, HsqlDialect(), LoggingInterceptor())
            return defaultSession.transaction {
                f(defaultSession)
            }
        } finally {
            connection.close()
        }
    }

    fun getInsertedSessions() = withSession {
        val sessions = it.select("select * from sessions") { row ->
            SessionInfo(row.string("thread"), row.long("connection"))
        }
        println(sessions.joinToString("\n"))
        sessions
    }

    test(expected = javaClass<IllegalStateException>()) fun `Uninitialised thread should be rejected`() {
        dao.insertCurrent()
    }

    test fun `An initialised session should successfully insert`() {
        ThreadLocalSession.initialise()
        dao.insertCurrent()
        ThreadLocalSession.finalise(commitTransaction = true)
        val sessions = getInsertedSessions()
        assertEquals(1, sessions.size())
    }

    test fun `An initialised session should not insert on rollback`() {
        ThreadLocalSession.initialise()
        dao.insertCurrent()
        ThreadLocalSession.finalise(commitTransaction = false)
        val sessions = getInsertedSessions()
        assertEquals(0, sessions.size())
    }

    test fun `Inserts on different threads should use different connections`() {
        val threads = 5
        val requests = 100

        val executor = Executors.newFixedThreadPool(threads)

        requests.times {
            executor.submit {
                ThreadLocalSession.initialise()
                try {
                    dao.insertCurrent()
                } finally {
                    ThreadLocalSession.finalise(commitTransaction = true)
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        val sessions = getInsertedSessions()
        assertEquals(requests, sessions.size())

        // Should use a different connection per request (will usually be pooled)
        assertEquals(requests, sessions.groupBy { it.connection }.size())

        // Should have executed in multiple threads
        assertEquals(threads, sessions.groupBy { it.thread }.size())
    }
}
