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
import org.apache.tomcat.jdbc.pool.DataSource
import kotlin.test.assertEquals
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
}

fun insert(session: Session, value: String) = session.update("insert into test(value) values (:value)", mapOf("value" to value))

class TransactionalInterceptorTest {
    companion object {
        val dataSource = DataSource().let { ds ->
            ds.setDefaultAutoCommit(true)
            ds.setDriverClassName("org.hsqldb.jdbc.JDBCDriver")
            ds.setUrl("jdbc:hsqldb:mem:transactional_test")
            ds
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
    val interfaceService: Service = transactionalProxyFactory.fromInterfaces(ServiceWithInterface(session))
    val service: ConcreteService = transactionalProxyFactory.fromClass(ConcreteService(session), listOf(ConcreteService::session))

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

    test fun `should rollback on exceptions by default`() {
        try {
            service.throwsRollbackDefault("value")
            fail()
        } catch(e: Exception) {
        }

        assertEquals(findAll(), listOf<String>())
    }

    test fun `should commit on ignored exceptions`() {
        try {
            service.throwsIgnore("value")
            fail()
        } catch(e: IllegalArgumentException) {
        }

        assertEquals(findAll(), listOf("value"))
    }
}