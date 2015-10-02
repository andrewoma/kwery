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
import com.github.andrewoma.kwery.mapper.*
import com.github.andrewoma.kwery.mappertest.AbstractSessionTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

private val table = "standard_mapping"

data class Standard(
        val boolean: Boolean,
        val byte: Byte,
        val short: Short,
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val decimal: BigDecimal,
        val string: String,
        val timestamp: Timestamp,
        val date: Date,
        val time: Time,
        val bytes: ByteArray,
        val optBoolean: Boolean?,
        val optByte: Byte?,
        val optShort: Short?,
        val optInt: Int?,
        val optLong: Long?,
        val optFloat: Float?,
        val optDouble: Double?,
        val optDecimal: BigDecimal?,
        val optString: String?,
        val optTimestamp: Timestamp?,
        val optDate: Date?,
        val optTime: Time?,
        val optBytes: ByteArray?,
        val blob: ByteArray,
        val clob: String,
        val intArray: List<Int>
) {
    override fun hashCode() = int

    override fun equals(other: Any?) = other is Standard
            && other.boolean == boolean
            && other.byte == byte
            && other.short == short
            && other.int == int
            && other.long == long
            && other.float == float
            && other.double == double
            && other.decimal == decimal
            && other.string == string
            && other.timestamp == timestamp
            && other.date == date
            && other.time == time
            && Arrays.equals(other.bytes, bytes)
            && other.optBoolean == optBoolean
            && other.optByte == optByte
            && other.optShort == optShort
            && other.optInt == optInt
            && other.optLong == optLong
            && other.optFloat == optFloat
            && other.optDouble == optDouble
            && other.optDecimal == optDecimal
            && other.optString == optString
            && other.optTimestamp == optTimestamp
            && other.optDate == optDate
            && other.optTime == optTime
            && Arrays.equals(other.optBytes, optBytes)
            && Arrays.equals(other.blob, blob)
            && other.clob == clob
            && other.intArray == intArray
}

object standardTable : Table<Standard, Int>(table) {
    // @formatter:off
    val BooleanCol         by col(Standard::boolean)
    val ByteCol            by col(Standard::byte)
    val ShortCol           by col(Standard::short)
    val IntCol             by col(Standard::int, id = true)
    val LongCol            by col(Standard::long)
    val FloatCol           by col(Standard::float)
    val DoubleCol          by col(Standard::double)
    val DecimalCol         by col(Standard::decimal)
    val StringCol          by col(Standard::string)
    val TimestampCol       by col(Standard::timestamp)
    val DateCol            by col(Standard::date)
    val TimeCol            by col(Standard::time)
    val BytesCol           by col(Standard::bytes)
    val OptBooleanCol      by col(Standard::optBoolean)
    val OptByteCol         by col(Standard::optByte)
    val OptShortCol        by col(Standard::optShort)
    val OptIntCol          by col(Standard::optInt)
    val OptLongCol         by col(Standard::optLong)
    val OptFloatCol        by col(Standard::optFloat)
    val OptDoubleCol       by col(Standard::optDouble)
    val OptDecimalCol      by col(Standard::optDecimal)
    val OptStringCol       by col(Standard::optString)
    val OptTimestampCol    by col(Standard::optTimestamp)
    val OptDateCol         by col(Standard::optDate)
    val OptTimeCol         by col(Standard::optTime)
    val OptBytesCol        by col(Standard::optBytes)
    val BlobCol            by col(Standard::blob, converter = blobConverter)
    val ClobCol            by col(Standard::clob, converter = clobConverter)
    val IntArrayCol        by col(Standard::intArray, converter = ArrayConverter<Int>("int"))
    // @formatter:on

    override fun idColumns(id: Int) = setOf(IntCol of id)

    override fun create(value: Value<Standard>): Standard = Standard(
            value of BooleanCol,
            value of ByteCol,
            value of ShortCol,
            value of IntCol,
            value of LongCol,
            value of FloatCol,
            value of DoubleCol,
            value of DecimalCol,
            value of StringCol,
            value of TimestampCol,
            value of DateCol,
            value of TimeCol,
            value of BytesCol,
            value of OptBooleanCol,
            value of OptByteCol,
            value of OptShortCol,
            value of OptIntCol,
            value of OptLongCol,
            value of OptFloatCol,
            value of OptDoubleCol,
            value of OptDecimalCol,
            value of OptStringCol,
            value of OptTimestampCol,
            value of OptDateCol,
            value of OptTimeCol,
            value of OptBytesCol,
            value of BlobCol,
            value of ClobCol,
            value of IntArrayCol
    )
}

class StandardDao(session: Session) : AbstractDao<Standard, Int>(session, standardTable, { it.int }, null, IdStrategy.Explicit)

class StandardConvertersMappingTest : AbstractSessionTest() {

    @Before fun before() {
        initialise(this.javaClass.simpleName) {
            session.update("""
                create table $table(
                     boolean_col   boolean not null,
                     byte_col      int not null,
                     short_col     int not null,
                     int_col       int not null,
                     long_col      bigint not null,
                     float_col     float not null,
                     double_col    double not null,
                     decimal_col   decimal(28,9) not null,
                     string_col    varchar(256) not null,
                     timestamp_col timestamp not null,
                     date_col      date not null,
                     time_col      time not null,
                     bytes_col     blob not null,
                     opt_boolean_col   boolean,
                     opt_byte_col      int,
                     opt_short_col     int,
                     opt_int_col       int,
                     opt_long_col      bigint,
                     opt_float_col     float,
                     opt_double_col    double,
                     opt_decimal_col   decimal(28,9),
                     opt_string_col    varchar(256),
                     opt_timestamp_col timestamp,
                     opt_date_col      date,
                     opt_time_col      time,
                     opt_bytes_col     blob,
                     blob_col          blob,
                     clob_col          clob,
                     int_array_col     int array,
                )
            """)
        }
    }

    @Test fun `should map standard values`() {
        val dao = StandardDao(session)
        val now = ZonedDateTime.now().withNano(0).toInstant()
        val inserted = dao.insert(Standard(
                true, 1, 2, 3, 4, 5.0f, 6.0, BigDecimal("23.450000000"), "string", Timestamp.from(now),
                Date.valueOf("2015-2-3"), Time.valueOf("13:01:02"), "ab".toByteArray(Charsets.US_ASCII),
                true, 1, 2, 3, 4, 5.0f, 6.0, BigDecimal("23.450000000"), "string", Timestamp.from(now),
                Date.valueOf("2015-2-3"), Time.valueOf("13:01:02"), "ab".toByteArray(Charsets.US_ASCII),
                "blob".toByteArray(Charsets.US_ASCII), "clob", listOf(1, 2, 3)
        ))
        val fetched = dao.findById(3)
        assertEquals(inserted, fetched)
    }
}