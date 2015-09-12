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

import com.github.andrewoma.kwery.core.dialect.PostgresDialect
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test

class ManagedThreadLocalSessionDelegationTest : AbstractSessionDelegationTest() {
    companion object {
        val session = ManagedThreadLocalSession(postgresDataSource, PostgresDialect(), postgresLoggingInterceptor)
    }

    override fun <R> withSession(f: (Session) -> R) = session.use(false) { f(session) }

    @Test fun `Manual transaction blocks should be honoured`() {
        withSession { session ->

            val t1 = session.manualTransaction()
            val key1 = session.insert(insertSql, mapOf("value" to "v1")) { it.int("key") }
            t1.commit()

            val t2 = session.manualTransaction()
            val key2 = session.insert(insertSql, mapOf("value" to "v2")) { it.int("key") }
            t2.rollback()

            assertNotNull(select(session, key1.second))
            assertNull(select(session, key2.second))
        }
    }
}
