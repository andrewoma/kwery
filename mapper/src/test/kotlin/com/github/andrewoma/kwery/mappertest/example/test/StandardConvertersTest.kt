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

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.mapper.AbstractDao
import com.github.andrewoma.kwery.mapper.IdStrategy
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.Value
import com.github.andrewoma.kwery.mappertest.AbstractSessionTest
import com.github.andrewoma.kwery.mappertest.example.tableConfig
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

private val table = "standard_converters"

data class Standard(
        val boolean: Boolean,
        val byte: Byte,
        val short: Short,
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double
)

object standardTable : Table<Standard, Int>(table, tableConfig) {
    // @formatter:off
    val BooleanCol         by col(Standard::boolean)
    val ByteCol            by col(Standard::byte)
    val ShortCol           by col(Standard::short)
    val IntCol             by col(Standard::int, id = true)
    val LongCol            by col(Standard::long)
    val FloatCol           by col(Standard::float)
    val DoubleCol          by col(Standard::double)
    // @formatter:on

    override fun idColumns(id: Int) = setOf(IntCol of id)

    override fun create(value: Value<Standard>): Standard = Standard(value of BooleanCol, value of ByteCol,
            value of ShortCol, value of IntCol, value of LongCol,
            value of FloatCol, value of DoubleCol)
}


class StandardDao(session: Session) : AbstractDao<Standard, Int>(session, standardTable, { it.int }, null, IdStrategy.Explicit) {
}

class StandardConvertersTest : AbstractSessionTest() {

    @Before fun before() {
        initialise(this.javaClass.simpleName) {
            session.update("""
                create table $table(
                     boolean_col boolean,
                     byte_col    int,
                     short_col   int,
                     int_col     int,
                     long_col    bigint,
                     float_col   float,
                     double_col  double,
                )
            """)
        }
    }

    @Test fun `should map standard values`() {
        val dao = StandardDao(session)
        val inserted = dao.insert(Standard(true, 1, 2, 3, 4, 5.0f, 6.0))
        val fetched = dao.findById(3)
        assertEquals(inserted, fetched)
    }
}