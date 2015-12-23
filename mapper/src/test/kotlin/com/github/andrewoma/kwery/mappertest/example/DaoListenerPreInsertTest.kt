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

package com.github.andrewoma.kwery.mappertest.example

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.mapper.*
import com.github.andrewoma.kwery.mapper.listener.Event
import com.github.andrewoma.kwery.mapper.listener.Listener
import com.github.andrewoma.kwery.mapper.listener.PreInsertEvent
import com.github.andrewoma.kwery.mapper.listener.PreUpdateEvent
import com.github.andrewoma.kwery.mappertest.AbstractSessionTest
import org.junit.Test
import java.time.LocalDateTime
import kotlin.properties.Delegates
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

data class Entity(val id: Int, val version: Int = 1, var dateCreated: LocalDateTime? = null, var dateModified: LocalDateTime? = null)

object entityTable : Table<Entity, Int>("entity", tableConfig), VersionedWithInt {
    val Id by col(Entity::id, id = true)
    val DateCreated by col(Entity::dateCreated)
    val DateModified by col(Entity::dateModified)
    val Version by col(Entity::version, version = true)

    override fun idColumns(id: Int) = setOf(Id of id)

    override fun create(value: Value<Entity>) = Entity(
            id = value of Id,
            version = value of Version,
            dateCreated = value of DateCreated,
            dateModified = value of DateModified
    )
}

class EntityDao(session: Session) : AbstractDao<Entity, Int>(
        session,
        entityTable,
        { it.id }
) {
    init {
        addListener(DateModifiedHandler())
    }

    inner class DateModifiedHandler : Listener {
        override fun onEvent(session: Session, events: List<Event>) {
            for (event in events) {
                if (event is PreInsertEvent && event.value is Entity) {
                    // TODO: figure out why smart casting doesn't work here
                    ((event.value) as Entity).dateCreated = LocalDateTime.now()
                    ((event.value) as Entity).dateModified = LocalDateTime.now()
                }

                if (event is PreUpdateEvent && event.new is Entity) {
                    ((event.new) as Entity).dateModified = LocalDateTime.now()
                }
            }
        }
    }
}

class DaoListenerPreInsertTest : AbstractSessionTest() {
    var dao: EntityDao by Delegates.notNull()
    var nextTransactionId = 0

    override var startTransactionByDefault = false

    override fun afterSessionSetup() {
        initialise("entity") {
            it.update("""
                create table entity(
                    id          identity,
                    version		integer not null,
                    date_created timestamp,
                    date_modified timestamp
                )
            """)
        }
        super.afterSessionSetup()

        dao = EntityDao(session)
    }

    @Test fun `dateCreated and dateModified should be populated by Listener`() {
        val entity = Entity(1);

        session.transaction {
            dao.insert(entity)
        }

        assertNotNull(entity.dateCreated)
        assertNotNull(entity.dateModified)

        val new = entity.copy()
        Thread.sleep(1)

        session.transaction {
            dao.update(entity, new)
        }

        println("${entity.dateModified} -> ${new.dateModified}")
        assertTrue(new.dateModified!!.isAfter(entity.dateModified))
    }

    @Test fun `dateCreated and dateModified should be populated by Batch Operations`() {
        val one = Entity(2);
        val two = Entity(3);

        session.transaction {
            dao.batchInsert(listOf(one, two))
        }

        assertNotNull(one.dateCreated)
        assertNotNull(one.dateModified)
        assertNotNull(two.dateCreated)
        assertNotNull(two.dateModified)

        Thread.sleep(1)
        val results = dao.batchUpdate(listOf(Pair(one, one.copy()), Pair(two, two.copy())))

        for (entity in results) {
            if (entity.id == one.id) {
                assertTrue(entity.dateModified!!.isAfter(one.dateModified))
            }

            if (entity.id == two.id) {
                assertTrue(entity.dateModified!!.isAfter(two.dateModified))
            }
        }
    }

    @Test fun `dateModified should be incremented by unsafeUpdate`() {
        val entity = Entity(4)

        session.transaction {
            dao.insert(entity)
        }

        val initialDateModified = entity.dateModified
        Thread.sleep(1)

        session.transaction {
            dao.unsafeUpdate(entity)
        }

        assertTrue(entity.dateModified!!.isAfter(initialDateModified))
    }

    @Test fun `dateModified should be incremented by unsafeBatchUpdate`() {
        val entity = Entity(5)

        session.transaction {
            dao.insert(entity)
        }

        val initialDateModified = entity.dateModified
        Thread.sleep(1)

        session.transaction {
            dao.unsafeBatchUpdate(listOf(entity))
        }

        assertTrue(entity.dateModified!!.isAfter(initialDateModified))
    }
}
