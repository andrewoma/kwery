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

package com.github.andrewoma.kwery.mappertest.example.test

import com.github.andrewoma.kwery.mappertest.example.*
import org.junit.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActorDaoTest : AbstractFilmDaoTest<Actor, Int, ActorDao>() {
    override var dao: ActorDao by Delegates.notNull()

    override fun afterSessionSetup() {
        dao = ActorDao(session, FilmActorDao(session))
        super.afterSessionSetup()
    }

    override val data = listOf(
            Actor(Name("John", "Wayne")),
            Actor(Name("Meg", "Ryan")),
            Actor(Name("Jeff", "Bridges"), ++staticId),
            Actor(Name("Yvonne", "Strahovsky"), ++staticId)
    )

    override fun mutateContents(t: Actor) = t.copy(name = Name("Bradley", "Cooper"))

    override fun contentsEqual(t1: Actor, t2: Actor) =
            t1.name == t2.name


    @Test fun `findByExample matches example given`() {
        insertAll()
        val result = dao.findByExample(Actor().copy(name = Name("", lastName = "Ryan")), setOf(actorTable.LastName))
        assertEquals(1, result.size())
        assertEquals("Meg", result.first().name.firstName)
    }

    @Test fun `findByLastNames matches multiple names`() {
        insertAll()
        val names = dao.findByLastNames(listOf("Ryan", "Bridges")).map { it.name }
        assertTrue(names.containsAll(setOf(Name("Jeff", "Bridges"), Name("Meg", "Ryan"))))
        assertFalse(names.contains(Name("Yvonne", "Strahovsky")))
    }

    @Test fun `Insert with default key should result in a generated key`() {
        val actor = data[0]
        val inserted = dao.insert(actor)
        assertTrue(contentsEqual(actor, inserted))
        assertFalse(actor.id == inserted.id)
    }

    @Test fun `Insert with non-default key should insert given key`() {
        val actor = data[3]
        val inserted = dao.insert(actor)
        assertTrue(contentsEqual(actor, inserted))
        assertTrue(actor.id == inserted.id)
    }
}