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

import com.github.andrewoma.kwery.core.dialect.MysqlDialect
import org.junit.Test
import kotlin.test.assertEquals

class MysqlDialectTest : AbstractDialectTest(mysqlDataSource, MysqlDialect()) {
    //language=MySQL
    override val sql = """
            drop table if exists dialect_test;

            create table dialect_test (
              id            varchar(255),
              time_col      time,
              date_col      date,
              timestamp_col timestamp(3),
              binary_col    blob,
              varchar_col   varchar(1000),
              blob_col      blob,
              clob_col      text,
              array_col     text -- Not supported
            ) engine innodb char set utf8;

            drop table if exists test;

            create table test (
              id            varchar(255),
              value         varchar(255)
            ) engine innodb char set utf8
        """

    @Test fun `Limits should be applied to variable parameters`() {
        assertEquals("'12'", dialect.bind("12345", 2))
        assertEquals("X'3132'", dialect.bind("12345".toByteArray(), 2))
    }
}