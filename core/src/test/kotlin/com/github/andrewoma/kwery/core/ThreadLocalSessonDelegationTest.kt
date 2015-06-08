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
import org.junit.Before as before
import org.junit.Test as test

class ThreadLocalSessionDelegationTest {
    companion object {
        var initialised = false
        val session = ThreadLocalSession(postgresDataSource, PostgresDialect(), postgresLoggingInterceptor)
    }

    val insertSql = "insert into delegate_test(value) values (:value)"

    before fun setUp() {
        if (!initialised) {
            initialised = true
            val sql = """
                drop table if exists delegate_test;
                create table delegate_test (
                    key serial,
                    value varchar(1000)
                )
            """
            session.use { for (statement in sql.split(";".toRegex())) session.update(statement) }
        }

        session.use { session.update("delete from delegate_test") }
    }

    test fun `Insert with generated keys`() {
        session.use {
            val (count, key) = session.insert(insertSql, mapOf("value" to "hello")) { row -> row.int("key") }
            assertEquals(1, count)
            assertEquals("hello", select(key))
        }
    }

    test fun `Rollback only should rollback transaction`() {
        val key = session.use {
            val sql = "insert into delegate_test(value) values (:value)"
            val (count, key) = session.insert(sql, mapOf("value" to "hello")) { row -> row.int("key") }
            session.currentTransaction?.rollbackOnly = true

            assertNotNull(session.connection)
            assertEquals(1, count)
            key
        }
        assertNull(session.use { select(key) })
    }

    test fun `Bind parameters should bind values`() {
        assertEquals("select '1'", session.use { session.bindParameters("select ':value'", mapOf("value" to 1)) })
    }

    test fun `forEach and stream should process each row`() {
        session.use {
            val sql = "insert into delegate_test(value) values (:value)"
            val parameters = listOf(
                    mapOf("value" to "hello"),
                    mapOf("value" to "there")
            )
            session.batchInsert(sql, parameters) { row -> row.int("key") }

            val values = hashSetOf<String>()
            val select = "select value from delegate_test"
            session.forEach(select) { row ->
                values.add(row.string("value"))
            }

            val expected = setOf("hello", "there")
            assertEquals(expected, values)
            assertEquals(expected, session.sequence(select) { it.map { it.string("value") }.toSet() })
        }
    }

    test fun `Options should be accessible`() {
        session.use {
            assertNotNull(session.connection)
            assertNotNull(session.dialect)
            assertNotNull(session.defaultStatementOptions)
        }
    }

    test fun `Insert with keys followed by batch update`() {
        session.use {
            val insert = "insert into delegate_test(key, value) values (:key, :value)"
            var count = session.update(insert, mapOf("key" to 100, "value" to "v1"))
            assertEquals(1, count)
            assertEquals("v1", select(100))

            count = session.update(insert, mapOf("key" to 101, "value" to "v2"))
            assertEquals(1, count)
            assertEquals("v2", select(101))

            val params = listOf(
                    mapOf("key" to 100, "value" to "newV1"),
                    mapOf("key" to 101, "value" to "newV2")
            )
            val counts = session.batchUpdate("update delegate_test set value = :value where key = :key", params)
            assertEquals(listOf(1, 1), counts)

            assertEquals("newV1", select(100))
            assertEquals("newV2", select(101))
        }
    }

    test fun `Transaction blocks should be honoured`() {
        session.use(startTransaction = false) {
            val key1 = session.transaction {
                session.insert(insertSql, mapOf("value" to "v1")) { it.int("key") }
            }

            val key2 = session.transaction {
                session.currentTransaction?.rollbackOnly = true
                session.insert(insertSql, mapOf("value" to "v2")) { it.int("key") }
            }

            assertNotNull(select(key1.second))
            assertNull(select(key2.second))
        }
    }

    test fun `Manual transaction blocks should be honoured`() {
        session.use(startTransaction = false) {

            val t1 = session.manualTransaction()
            val key1 = session.insert(insertSql, mapOf("value" to "v1")) { it.int("key") }
            t1.commit()

            val t2 = session.manualTransaction()
            val key2 = session.insert(insertSql, mapOf("value" to "v2")) { it.int("key") }
            t2.rollback()

            assertNotNull(select(key1.second))
            assertNull(select(key2.second))
        }
    }

    fun select(key: Int): String? {
        return session.select("select value from delegate_test where key = :key", mapOf("key" to key)) { row ->
            row.string("value")
        }.firstOrNull()
    }
}
