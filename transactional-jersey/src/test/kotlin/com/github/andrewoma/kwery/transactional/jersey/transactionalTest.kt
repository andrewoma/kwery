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

package com.github.andrewoma.kwery.transactional.jersey

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.ThreadLocalSession
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.github.andrewoma.kwery.core.util.with
import io.dropwizard.testing.junit.ResourceTestRule
import org.apache.tomcat.jdbc.pool.DataSource
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.test.assertEquals
import kotlin.test.assertTrue

Produces(MediaType.APPLICATION_JSON)
Consumes(MediaType.APPLICATION_JSON)
interface Resource

Path("/class")
transactional class ClassLevelResource(val session: Session) : Resource {
    GET Path("/success") fun success() {
        insert(session, "value")
    }

    GET Path("/fail") fun fail(): Response {
        insert(session, "value")
        return Response.serverError().build()
    }
}

Path("/method")
class MethodLevelResource(val session: Session) : Resource {
    transactional GET Path("/success") fun success() {
        insert(session, "value")
    }

    transactional GET Path("/fail") fun fail(): Response {
        insert(session, "value")
        return Response.serverError().build()
    }

    GET Path("/no-annotation") fun none() {
        insert(session, "value")
    }
}

fun insert(session: Session, value: String) = session.update("insert into test(value) values (:value)", mapOf("value" to value))

class TransactionalTest {
    companion object {
        val dataSource = DataSource().with {
            setDefaultAutoCommit(true)
            setDriverClassName("org.hsqldb.jdbc.JDBCDriver")
            setUrl("jdbc:hsqldb:mem:transactional_test")
        }
    }

    Before fun initialise() {
        session.use(true) {
            session.update("create table if not exists test(value varchar(200))")
            session.update("delete from test")
        }
    }

    val session = ThreadLocalSession(dataSource, HsqlDialect(), LoggingInterceptor())

    val resources = ResourceTestRule.builder()
            .addResource(ClassLevelResource(session))
            .addResource(MethodLevelResource(session))
            .addProvider(TransactionListener())
            .build()

    fun findAll() = session.use(true) {
        session.select("select value from test") { row -> row.string("value")}
    }

    Rule fun resourcesRule() = resources

    Test fun `should commit with class annotation`() {
        val response = resources.client().target("/class/success").request().get()
        assertEquals(response.getStatus(), 204)
        assertEquals(findAll(), listOf("value"))
    }

    Test fun `should rollback on failure with class annotation`() {
        val response = resources.client().target("/class/fail").request().get()
        assertEquals(response.getStatus(), 500)
        assertTrue(findAll().isEmpty())
    }

    Test fun `should commit with method annotation`() {
        val response = resources.client().target("/method/success").request().get()
        assertEquals(response.getStatus(), 204)
        assertEquals(findAll(), listOf("value"))
    }

    Test fun `should rollback on failure with method annotation`() {
        val response = resources.client().target("/method/fail").request().get()
        assertEquals(response.getStatus(), 500)
        assertTrue(findAll().isEmpty())
    }

    Test fun `should fail with no annotation`() {
        val response = resources.client().target("/method/fail").request().get()
        assertEquals(response.getStatus(), 500)
        assertTrue(findAll().isEmpty())
    }
}
