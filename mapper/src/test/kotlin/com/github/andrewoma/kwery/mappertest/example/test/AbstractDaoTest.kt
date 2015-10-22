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

import com.github.andrewoma.kwery.mapper.AbstractDao
import com.github.andrewoma.kwery.mapper.IdStrategy
import com.github.andrewoma.kwery.mapper.OptimisticLockException
import com.github.andrewoma.kwery.mappertest.AbstractSessionTest
import org.junit.Test
import kotlin.test.*

abstract class AbstractDaoTest<T : Any, ID : Any, D : AbstractDao<T, ID>>() : AbstractSessionTest() {
    abstract var dao: D

    abstract val data: List<T>

    abstract fun mutateContents(t: T): T

    abstract fun contentsEqual(t1: T, t2: T): Boolean

    val dataWithKeys: List<T> by lazy(LazyThreadSafetyMode.NONE) { data.filter { id(it) != dao.defaultId } }

    val dataWithoutKeys: List<T> by lazy(LazyThreadSafetyMode.NONE) { data.filter { id(it) == dao.defaultId } }

    fun id(value: T) = dao.id(value)

    override fun afterSessionSetup() {
        dao.session.update("delete from ${dao.table.name} where ${dao.table.idColumns.map { "${it.name} > -1000" }.joinToString(" or ")}")
    }

    @Test fun `Insert with generated key should return a new key`() {
        if (dataWithoutKeys.isEmpty()) return

        val created = dataWithoutKeys.first()
        val inserted = dao.insert(created, IdStrategy.Generated)
        val found = dao.findById(id(inserted))
        println("found: $found")

        assertTrue(contentsEqual(created, found!!))
        assertTrue(id(created) != id(found))
    }

    @Test fun `Insert with key should preserve given key`() {
        if (dataWithKeys.isEmpty()) return

        val created = dataWithKeys.first()
        val inserted = dao.insert(created, IdStrategy.Explicit)
        val updated = dao.update(inserted, mutateContents(inserted))
        val found = dao.findById(id(updated))

        assertTrue(contentsEqual(updated, found!!))
        assertTrue(id(created) == id(found))
    }

    @Test fun `Find all should include inserted`() {
        val existing = dao.findAll().size
        val inserted = insert()

        val found = dao.findAll()
        assertEquals(existing + 1, found.size)
        assertNotNull(found.firstOrNull { id(it) == id(inserted) })
    }

    @Test fun `Find by ids with empty ids should return empty map`() {
        assertTrue(dao.findByIds(listOf()).isEmpty())
    }

    @Test fun `Find by ids with single id should return map`() {
        val inserted = insert()
        val found = dao.findByIds(listOf(id(inserted)))
        assertContentEquals(mapOf(id(inserted) to inserted), found)
    }

    @Test fun `Find by ids with single unknown id should return empty map`() {
        val id = id(insert())
        dao.delete(id)
        val found = dao.findByIds(listOf(id))
        assertTrue(found.isEmpty())
    }

    @Test fun `Find by ids should return values by id`() {
        if (dao.table.idColumns.size > 1) return // Unsupported for now

        val inserted = insert(3)

        val first = inserted.take(1).map { id(it) to it }.toMap()
        assertContentEquals(first, dao.findByIds(first.keys))

        val rest = inserted.drop(1).map { id(it) to it }.toMap()
        assertContentEquals(rest, dao.findByIds(rest.keys))
    }

    fun assertContentEquals(expected: Map<ID, T>, actual: Map<ID, T>) {
        assertEquals(expected.keys, actual.keys)
        for ((id, value) in expected) {
            assertTrue(contentsEqual(value, actual.get(id)!!))
        }
    }

    @Test fun `Insert batch with generated keys should return keys`() {
        if (dataWithoutKeys.isEmpty()) return

        val inserted = dao.batchInsert(dataWithoutKeys, IdStrategy.Generated)

        assertEquals(dataWithoutKeys.size, inserted.size)
        for ((old, new) in dataWithoutKeys.zip(inserted)) {
            assertTrue(contentsEqual(old, new))
            assertFalse(id(old) == id(new))
        }
    }

    @Test fun `Insert batch with ids return ids`() {
        if (dataWithKeys.isEmpty()) return

        val inserted = dao.batchInsert(dataWithKeys, IdStrategy.Explicit)
        assertEquals(dataWithKeys.size, inserted.size)
        for ((old, new) in dataWithKeys.zip(inserted)) {
            assertTrue(contentsEqual(old, new))
            assertEquals(id(old), id(new))
        }
    }

    fun insert() = insert(1).first()
    fun insertAll() = insert(-1)

    fun insert(count: Int): List<T> {
        val result = arrayListOf<T>()
        for (value in data) {
            result.add(dao.insert(value, IdStrategy.Auto))
            if (count != -1 && result.size == count) return result
        }

        check(result.size >= count) { "Not enough test data" }
        return result
    }

    @Test fun `Update should overwrite content`() {
        val inserted = insert()
        val updated = dao.update(inserted, mutateContents(inserted))
        val found = dao.findById(id(updated))

        assertTrue(contentsEqual(updated, found!!))
        assertEquals(id(inserted), id(found))
    }

    @Test fun `Update with delta should overwrite content`() {
        val inserted = insert()
        Thread.sleep(2)
        val updated = dao.update(inserted, mutateContents(inserted), deltaOnly = true)
        val found = dao.findById(id(updated))

        assertTrue(contentsEqual(updated, found!!))
        assertEquals(id(inserted), id(found))
    }

    @Test fun `Versioned update should increment version`() {
        if (dao.table.versionColumn == null) return


    }

    @Test fun `Delete should remove a row`() {
        val inserted = insert()
        assertNotNull(dao.findById(id(inserted)))
        assertEquals(1, dao.delete(id(inserted)))
        assertNull(dao.findById(id(inserted)))
    }

    //    test(expected = javaClass<OptimisticLockException>()) TODO - breaks incremental compiler
    @Test fun `Versioned update of same version should be rejected`() {
        if (dao.table.versionColumn == null) throw OptimisticLockException("")

        val inserted = insert()
        Thread.sleep(2)
        dao.update(inserted, mutateContents(inserted))
        Thread.sleep(2)
        try {
            dao.update(inserted, mutateContents(inserted))
            fail("OptimisticLockException expected") // Using annotation
        } catch(e: OptimisticLockException) {
        }
    }

    @Test fun `Unsafe update of same version should be accepted`() {
        val inserted = insert()

        Thread.sleep(2)
        assertEquals(1, dao.unsafeUpdate(mutateContents(inserted))) // This mutation will be silently lost

        Thread.sleep(2)
        val updated = mutateContents(inserted)
        assertEquals(1, dao.unsafeUpdate(updated))
        val found = dao.findById(id(updated))
        assertTrue(contentsEqual(updated, found!!))
    }

    @Test fun `Batch updates should update all values`() {
        if (dao.table.versionColumn == null || dao.table.idColumns.size > 1) return

        val inserted = insert(2)
        val mutated = inserted.map { mutateContents(it) }

        val updated = dao.batchUpdate(inserted.zip(mutated))
        assertEquals(2, updated.size)

        val found = dao.findByIds(updated.map { id(it) })

        for (value in updated) {
            assertTrue(contentsEqual(value, found[id(value)]!!))
        }
    }

    @Test fun `Unsafe batch updates should update all values`() {
        if (dao.table.idColumns.size > 1) return

        val inserted = insert(2)
        val mutated = inserted.map { mutateContents(it) }

        dao.unsafeBatchUpdate(mutated)

        val found = dao.findByIds(inserted.map { id(it) })

        for (value in mutated) {
            assertTrue(contentsEqual(value, found[id(value)]!!))
        }
    }

    @Test fun `Batch updates should reject updates of same version`() {
        if (dao.table.versionColumn == null) return

        val inserted = insert(2)
        val mutated = inserted.map { mutateContents(it) }

        Thread.sleep(2)
        val updated = dao.batchUpdate(inserted.zip(mutated))
        assertEquals(2, updated.size)

        Thread.sleep(2)
        try {
            dao.batchUpdate(inserted.zip(mutated)) // Attempt to original collection again
            fail("Expected OptimisticLockException")
        } catch(e: OptimisticLockException) {
        }
    }

    @Test fun `Allocate ids should contain a unique sequence of ids`() {
        if (!dialect.supportsAllocateIds || dao.table.sequence == null) return

        val count = 100
        val ids = dao.allocateIds(count)

        assertEquals(count, ids.size)
    }
}