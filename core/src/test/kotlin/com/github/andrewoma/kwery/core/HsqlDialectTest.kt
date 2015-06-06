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

import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import kotlin.test.assertEquals
import org.junit.Test as test

class HsqlDialectTest : AbstractDialectTest(hsqlDataSource, HsqlDialect()) {
    //language=SQL
    override val sql = """
            create table dialect_test (
              id            varchar(255),
              time_col      time,
              date_col      date,
              timestamp_col timestamp,
              binary_col    binary(6),
              varchar_col   varchar(1000),
              blob_col      blob,
              clob_col      clob,
              array_col     int array
            );

            create table test (
              id            varchar(255),
              value         varchar(255)
            );

            create sequence test_seq
        """

    test fun `Limits should be applied to variable parameters`() {
        assertEquals("'12'", dialect.bind("12345", 2))
        assertEquals("X'3132'", dialect.bind("12345".toByteArray(), 2))

        val clob = session.connection.createClob()
        clob.setString(1, "12345")
        assertEquals("'12'", dialect.bind(clob, 2))

        val blob = session.connection.createBlob()
        blob.setBytes(1, "12345".toByteArray())
        assertEquals("X'3132'", dialect.bind(blob, 2))

        val array = session.connection.createArrayOf("varchar", arrayOf("12345"))
        assertEquals("array['12']", dialect.bind(array, 2))
    }
}