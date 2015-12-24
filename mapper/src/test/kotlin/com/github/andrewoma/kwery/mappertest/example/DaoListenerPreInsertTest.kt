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
import com.github.andrewoma.kwery.mapper.AbstractDao
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.Value
import com.github.andrewoma.kwery.mapper.VersionedWithInt
import com.github.andrewoma.kwery.mapper.listener.*
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
    val listener = DateModifiedHandler()

    init {
        addListener(listener)
    }

    inner class DateModifiedHandler : Listener {
        override fun onEvent(session: Session, event: Event) {
            if (event is TransformingEvent && event.new is Entity) {
                val entity = event.new as Entity
                if (event is PreInsertEvent) {
                    entity.dateCreated = LocalDateTime.now()
                    entity.dateModified = LocalDateTime.now()
                }
                if (event is PreUpdateEvent && event.new is Entity) {
                    entity.dateModified = LocalDateTime.now()
                }
            }
        }
    }
}

open class DaoListenerPreInsertTest : AbstractSessionTest() {
    var dao: EntityDao by Delegates.notNull()

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
        session.update("delete from entity")

        dao = EntityDao(session)
    }

    @Test fun `dateCreated and dateModified should be populated by Listener`() {
        var entity = Entity(1)

        session.transaction {
            entity = dao.insert(entity)
        }

        assertNotNull(entity.dateCreated)
        assertNotNull(entity.dateModified)

        var new = entity.copy()
        Thread.sleep(1)

        session.transaction {
            new = dao.update(entity, new)
        }

        println("${entity.dateModified} -> ${new.dateModified}")
        assertTrue(new.dateModified!!.isAfter(entity.dateModified))
    }

    @Test fun `dateCreated and dateModified should be populated by Batch Operations`() {
        var one = Entity(2)
        var two = Entity(3)

        session.transaction {
            val result = dao.batchInsert(listOf(one, two))
            one = result[0]
            two = result[1]
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
        var entity = Entity(4)

        session.transaction {
            entity = dao.insert(entity)
        }

        val initialDateModified = entity.dateModified
        Thread.sleep(1)

        session.transaction {
            entity = dao.unsafeUpdate(entity)
        }

        assertTrue(entity.dateModified!!.isAfter(initialDateModified))
    }

    @Test fun `dateModified should be incremented by unsafeBatchUpdate`() {
        var entity = Entity(5)

        session.transaction {
            entity = dao.insert(entity)
        }

        val initialDateModified = entity.dateModified
        Thread.sleep(1)

        session.transaction {
            entity = dao.unsafeBatchUpdate(listOf(entity)).first()
        }

        assertTrue(entity.dateModified!!.isAfter(initialDateModified))
    }
}

class DaoListenerPreInsertTransformingTest : DaoListenerPreInsertTest() {
    override fun afterSessionSetup() {
        super.afterSessionSetup()
        dao.removeListener(dao.listener)
        dao.addListener(object : Listener {
            override fun onEvent(session: Session, event: Event) {
                if (event is TransformingEvent && event.new is Entity) {
                    val entity = event.new as Entity
                    if (event is PreInsertEvent) {
                        event.transformed = entity.copy(dateCreated = LocalDateTime.now(), dateModified = LocalDateTime.now())
                    }
                    if (event is PreUpdateEvent && event.new is Entity) {
                        event.transformed = entity.copy(dateModified = LocalDateTime.now())
                    }
                }
            }
        })
    }
}