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

package com.github.andrewoma.kwery.core.interceptor

import com.github.andrewoma.kwery.core.ExecutingStatement
import org.junit.Test as test
import com.github.andrewoma.kwery.core.ThreadLocalSession
import com.github.andrewoma.kwery.core.hsqlDataSource
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.SelectOptions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementInterceptorChainTest {
    val calls = arrayListOf<String>()
    val statement = ExecutingStatement(ThreadLocalSession(hsqlDataSource, HsqlDialect()), hashMapOf(), "sql", listOf(), SelectOptions())

    inner class Interceptor(val name: String) : StatementInterceptor {
        override fun executed(statement: ExecutingStatement) {
            calls.add("$name.executed")
        }
    }

    class MappedException() : Exception()

    class ExceptionInterceptor : StatementInterceptor {
        override fun exception(statement: ExecutingStatement, e: Exception) = MappedException()
    }

    test fun `Each interceptor in chain should be invoked`() {
        val chain = StatementInterceptorChain(listOf(Interceptor("1"), Interceptor("2")))
        chain.executed(statement)

        assertEquals(listOf("1.executed", "2.executed"), calls)
    }

    test fun `Transformed exceptions should be propagated from inner`() {
        val chain = StatementInterceptorChain(listOf(ExceptionInterceptor(), Interceptor("2")))
        val result = chain.exception(statement, Exception())

        assertTrue(result is MappedException)
    }

    test fun `Transformed exceptions should be propagated from outer`() {
        val chain = StatementInterceptorChain(listOf(Interceptor("2"), ExceptionInterceptor()))
        val result = chain.exception(statement, Exception())

        assertTrue(result is MappedException)
    }
}