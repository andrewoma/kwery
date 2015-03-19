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

import org.junit.Assert.assertEquals
import java.math.BigDecimal
import java.sql.SQLException
import java.sql.Time
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test as test

open class RowTest : AbstractSessionTest() {
    companion object {
        var initialised = false
    }

    override fun afterSessionSetup() {
        val sql = """
            create table row_test (
                int_col int,
                boolean_col boolean,
                decimal_col decimal(10,1),
                double_col double,
                time_col time,
                date_col date,
                timestamp_col timestamp,
                binary_col binary(1000),
                varchar_col varchar(1000),
                blob_col blob,
                clob_col clob,
                array_col int array
            );

            insert into row_test(int_col, boolean_col, decimal_col, double_col, time_col, date_col, timestamp_col,
                    binary_col, varchar_col, blob_col, clob_col, array_col)
                values(null, null, null, null, null, null, null, null, null, null, null, null);

            insert into row_test(int_col, boolean_col, decimal_col, double_col, time_col, date_col, timestamp_col,
                    binary_col, varchar_col, blob_col, clob_col, array_col)
                values(1, true, 2.1, 3.1, '13:01:02', '2014-12-01', '2013-11-02 12:00:02.33', X'4142',
                    'varchar', X'4243', 'clob', ARRAY[1, 2])
        """
        if (!initialised) {
            for (statement in sql.split(";")) {
                session.update(statement)
            }
            initialised = true
        }
    }

    test fun `null values should be null`() {
        session.select("select * from row_test where int_col is null") { row ->
            assertNull(row.booleanOrNull("boolean_col"))
            assertNull(row.byteOrNull("int_col"))
            assertNull(row.shortOrNull("int_col"))
            assertNull(row.intOrNull("int_col"))
            assertNull(row.longOrNull("int_col"))
            assertNull(row.floatOrNull("decimal_col"))
            assertNull(row.doubleOrNull("decimal_col"))
            assertNull(row.bigDecimalOrNull("decimal_col"))
            assertNull(row.timeOrNull("time_col"))
            assertNull(row.dateOrNull("date_col"))
            assertNull(row.dateOrNull("timestamp_col"))
            assertNull(row.timestampOrNull("timestamp_col"))
            assertNull(row.objectOrNull("timestamp_col"))
            assertNull(row.stringOrNull("varchar_col"))
            assertNull(row.clobOrNull("clob_col"))
            assertNull(row.blobOrNull("blob_col"))
            assertNull(row.bytesOrNull("binary_col"))
            assertNull(row.characterStreamOrNull("clob_col"))
            assertNull(row.binaryStreamOrNull("blob_col"))
            assertNotNull(row.array<Int>("array_col"))
        }
    }

    test fun `non null values should match inserted`() {
        session.select("select * from row_test where int_col is not null") { row ->
            assertEquals(1.toByte(), row.byte("int_col"))
            assertEquals(1.toShort(), row.short("int_col"))
            assertEquals(1.toInt(), row.int("int_col"))
            assertEquals(1.toLong(), row.long("int_col"))
            assertEquals(true, row.boolean("boolean_col"))
            assertEquals(2.1.toFloat(), row.float("decimal_col"))
            assertEquals(2.1.toDouble(), row.double("decimal_col"), 0.001)
            assertEquals(BigDecimal("2.1"), row.bigDecimal("decimal_col"))
            assertEquals(Time(13, 1, 2), row.time("time_col"))
            assertEquals(LocalDate.of(2014, 12, 1), row.date("date_col").toLocalDate())
            assertEquals(LocalDate.of(2014, 12, 1), (row.obj("date_col") as java.sql.Date).toLocalDate())
            assertEquals(LocalDateTime.of(2013, 11, 2, 12, 0, 2, 330000000), row.timestamp("timestamp_col").toLocalDateTime())
            assertEquals("varchar", row.string("varchar_col"))
            assertEquals("clob", row.clob("clob_col").getCharacterStream().readText())
            assertEquals("clob", row.characterStream("clob_col").readText())
            assertEquals("AB", String(row.bytes("binary_col"), Charsets.US_ASCII).trim())
            assertEquals("BC", String(row.blob("blob_col").getBinaryStream().readBytes(), Charsets.US_ASCII))
            assertEquals("BC", String(row.binaryStream("blob_col").readBytes(), Charsets.US_ASCII))
            assertEquals(listOf(1, 2), row.array<Int>("array_col"))
        }
    }

    test(expected = javaClass<SQLException>()) fun `invalid column name should be rejected`() {
        session.select("select * from row_test where int_col is null") { row ->
            row.booleanOrNull("does_not_exist")
        }
    }

    test(expected = javaClass<IllegalArgumentException>()) fun `null encountered fetching a nonnull column should be rejected`() {
        session.select("select * from row_test where int_col is null") { row ->
            row.boolean("boolean_col")
        }
    }
}