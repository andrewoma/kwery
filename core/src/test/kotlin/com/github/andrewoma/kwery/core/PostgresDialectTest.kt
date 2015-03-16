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
import org.junit.Test as test
import kotlin.test.assertEquals

class PostgresDialectTest : AbstractDialectTest(postgresDataSource, PostgresDialect()) {
    //language=SQL
    override val sql = """
            drop table if exists dialect_test;

            create table dialect_test (
              id            varchar(255),
              time_col      time,
              date_col      date,
              timestamp_col timestamp,
              binary_col    bytea,
              varchar_col   varchar(1000),
              blob_col      oid,
              clob_col      text, -- Postgres doesn't have a CLOB type
              array_col     int array
            );

            drop table if exists test;

            create table test (
              id            varchar(255),
              value         varchar(255)
            );

            drop sequence if exists test_seq;
            create sequence test_seq;
        """

    test fun `Limits should be applied to variable parameters`() {
        assertEquals("'12'", dialect.bind("12345", 2))
        assertEquals("decode('MTI=','base64')", dialect.bind("12345".toByteArray(), 2))

        val array = session.connection.createArrayOf("varchar", array("12345"))
        assertEquals("array['12']", dialect.bind(array, 2))
    }
}