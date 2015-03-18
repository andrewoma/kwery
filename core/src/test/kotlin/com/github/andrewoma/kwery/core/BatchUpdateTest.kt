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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BatchUpdateTest : AbstractFilmSessionTest() {

    test fun `insert rows in a batch`() {
        var nextId = 10000
        session.update("delete from actor where actor_id >= $nextId")

        val actors = listOf(
                Actor("Andrew", "O'Malley", ++nextId),
                Actor("Bill", "Murray", ++nextId)
        )

        val sql = "insert into actor(actor_id, first_name, last_name, last_update) " +
                "values (:actor_id, :first_name, :last_name, :last_update)"

        val list = session.batchUpdate(sql, actors.map { it.toMap() })
        assertEquals(listOf(1, 1), list)

        val found = selectActors(setOf(nextId, --nextId))
        assertEquals(actors, found)
    }

    test fun `insert rows in a batch with generated keys`() {
        val actors = listOf(
                Actor("Andrew", "O'Malley", 0),
                Actor("Bill", "Murray", 0),
                Actor("Ted", "Murray", 0)
        )

        val sql = "insert into actor(first_name, last_name, last_update) " +
                "values (:first_name, :last_name, :last_update)"

        val list = session.batchInsert(sql, actors.map { it.toMap() }) { row -> row.resultSet.getInt(1) }
        assertEquals(3, list.size())
        list.forEach {
            assertEquals(1, it.first)
            assertNotNull(it.second)
        }
    }
}
