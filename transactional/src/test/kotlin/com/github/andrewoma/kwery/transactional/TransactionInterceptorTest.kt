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

package com.github.andrewoma.kwery.transactional

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.ThreadLocalSession
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.github.andrewoma.kwery.core.util.apply
import org.apache.tomcat.jdbc.pool.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Before as before
import org.junit.Test as test

interface Service {
    fun insert(value: String): Int
}

transactional class ServiceWithInterface(val session: Session) : Service {
    override fun insert(value: String) = insert(session, value)
}

transactional open class ConcreteService(val session: Session) {
    open fun insert(value: String) = insert(session, value)

    open fun throwsRollbackDefault(value: String) {
        insert(session, value)
        throw Exception("rollback")
    }

    transactional(ignore = arrayOf(IllegalArgumentException::class))
    open fun throwsIgnore(value: String) {
        insert(session, value)
        throw IllegalArgumentException("ignore")
    }

    transactional(manual = true) open fun manualTransactions() {
        session.transaction { insert(session, "value1") }
        session.transaction { insert(session, "value2"); session.currentTransaction?.rollbackOnly = true }
        session.transaction { insert(session, "value3") }
    }
}

transactional open class Outer(val session: Session, val inner: Inner) {
    open fun bothInsert(innerValue: String, outerValue: String) {
        insert(session, outerValue)
        inner.insert(innerValue)
    }
    open fun outerFails(innerValue: String, outerValue: String) {
        inner.insert(outerValue)
        throw Exception("outer")

    }
    open fun innerFails(innerValue: String, outerValue: String) {
        insert(session, outerValue)
        inner.fail()
    }
}

transactional open class Inner(val session: Session) {
    open fun insert(value: String) = insert(session, value)
    open fun fail() = throw Exception("inner")
}

fun insert(session: Session, value: String) = session.update("insert into test(value) values (:value)", mapOf("value" to value))

class TransactionalInterceptorTest {
    companion object {
        val dataSource = DataSource().apply {
            setDefaultAutoCommit(true)
            setDriverClassName("org.hsqldb.jdbc.JDBCDriver")
            setUrl("jdbc:hsqldb:mem:transactional_test")
        }
    }

    before fun initialise() {
        session.use(true) {
            //language = SQL
            session.update("create table if not exists test(value varchar(200))")
            session.update("delete from test")
        }
    }

    val session = ThreadLocalSession(dataSource, HsqlDialect(), LoggingInterceptor())

    val interfaceService: Service = transactionalFactory.fromInterfaces(ServiceWithInterface(session))
    val service: ConcreteService = transactionalFactory.fromClass(ConcreteService(session), ConcreteService::session)

    val inner = transactionalFactory.fromClass(Inner(session), Inner::session)
    val outer = transactionalFactory.fromClass(Outer(session, inner), Outer::session, Outer::inner)


    fun findAll() = session.use(true) {
        session.select("select value from test") { row -> row.string("value")}
    }

    test fun `should intercept concrete classes`() {
        service.insert("value")
        assertEquals(findAll(), listOf("value"))
    }

    test fun `should intercept classes via interfaces`() {
        interfaceService.insert("value")
        assertEquals(findAll(), listOf("value"))
    }

    test fun `should support manual transactions`() {
        service.manualTransactions()
        assertEquals(findAll(), listOf("value1", "value3"))
    }

    test fun `should rollback on exceptions by default`() {
        try {
            service.throwsRollbackDefault("value")
            fail()
        } catch(e: Exception) {
        }

        assertTrue(findAll().isEmpty())
    }

    test fun `should commit on ignored exceptions`() {
        try {
            service.throwsIgnore("value")
            fail()
        } catch(e: IllegalArgumentException) {
        }

        assertEquals(findAll(), listOf("value"))
    }

    test fun `should support nested services`() {
        outer.bothInsert("value1", "value2")
        assertEquals(findAll().toSet(), setOf("value1", "value2"))
    }

    test fun `should roll back both if inner service fails`() {
        try {
            outer.innerFails("value1", "value2")
            fail()
        } catch(e: Exception) {
        }
        assertTrue(findAll().isEmpty())
    }

    test fun `should roll back both if outer service fails`() {
        try {
            outer.outerFails("value1", "value2")
            fail()
        } catch(e: Exception) {
        }
        assertTrue(findAll().isEmpty())
    }
}