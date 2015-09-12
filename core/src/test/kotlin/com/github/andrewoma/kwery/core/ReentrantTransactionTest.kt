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

import org.junit.Test
import kotlin.test.assertEquals

class ReentrantTransactionTest : AbstractFilmSessionTest() {
    override var startTransactionByDefault: Boolean = false

    override fun afterSessionSetup() {
        super.afterSessionSetup()
        session.update("delete from actor")
    }

    fun insertOneThenFail(name: String) = session.transaction { t ->
        insert(Actor(name, "$name 1"))
        t.rollbackOnly = true
    }

    fun insertMultiple(name: String) = session.transaction {
        insert(Actor(name, "$name 2"))
        insert(Actor(name, "$name 3"))
    }

    @Test fun `should support separate transactions`() {
        insertMultiple("Bill")
        insertMultiple("Ben")
        assertEquals(4, countActors())
    }

    @Test fun `should support reentrant`() {
        session.transaction {
            insertMultiple("Bill")
            insertMultiple("Ben")
        }
        assertEquals(4, countActors())
    }

    @Test fun `should rollback all if inner transaction fails`() {
        session.transaction {
            insertMultiple("Bill")
            insertOneThenFail("Ben")
        }
        assertEquals(0, countActors())
    }

    @Test fun `should rollback only failed transaction`() {
        insertMultiple("Bill")
        insertOneThenFail("Ben")
        assertEquals(2, countActors())
    }

    private fun countActors() = session.select("select count(*) c from actor") { it.int("c") }.single()
}