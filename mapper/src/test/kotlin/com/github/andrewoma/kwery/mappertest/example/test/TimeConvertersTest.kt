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

import com.github.andrewoma.kwery.core.Row
import com.github.andrewoma.kwery.mapper.*
import com.github.andrewoma.kwery.mapper.DurationConverter
import com.github.andrewoma.kwery.mappertest.AbstractSessionTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.time.*
import java.time.temporal.ChronoUnit.*
import java.time.temporal.TemporalUnit
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Before
import org.junit.Test

class TimeConvertersTest : AbstractSessionTest() {
    val table = "time_converters"

    Before fun before() {
        initialise(this.javaClass.getSimpleName()) {
            session.update("""
                create table $table(
                     timestamp_col timestamp,
                     date_col      date,
                     time_col      time,
                     bigint_col    bigint,
                     decimal_col   decimal(28,9),
                )
            """)
        }
    }

    @Test fun `should convert LocalDateTime`() {
        assertConversion(localDateTimeConverter, LocalDateTime.now(), "timestamp_col")
    }

    @Test fun `should convert LocalDate`() {
        assertConversion(localDateConverter, LocalDate.now(), "date_col")
    }

    @Test fun `should convert LocalTime`() {
        assertConversion(localTimeConverter, LocalTime.now().withNano(0), "time_col")
    }

    @Test fun `should convert Instant`() {
        assertConversion(instantConverter, Instant.now(), "timestamp_col")
    }

    @Test fun `should convert Duration and store as same unit`() {
        fun assertDuration(unit: TemporalUnit) = assertConversion(DurationConverter(unit),
                Duration.of(3, unit), "bigint_col", { row -> row.long("bigint_col") == 3L})

        assertDuration(NANOS)
        assertDuration(MICROS)
        assertDuration(MILLIS)
        assertDuration(SECONDS)
        assertDuration(MINUTES)
        assertDuration(HOURS)
        assertDuration(HALF_DAYS)
        assertDuration(DAYS)
    }

    @Test fun `should convert Duration to BigDecimal`() {
        fun assertDuration(value: Duration) =  assertConversion(durationToBigDecimalConverter, value, "decimal_col")

        assertDuration(Duration.ZERO)
        assertDuration(Duration.of(1, NANOS))
        assertDuration(Duration.of(1, MICROS))
        assertDuration(Duration.of(1, MILLIS))
        assertDuration(Duration.of(1, SECONDS))
        assertDuration(Duration.of(1, MINUTES))
        assertDuration(Duration.of(1, HOURS))
        assertDuration(Duration.of(1, HALF_DAYS))
        assertDuration(Duration.of(1, DAYS))
        assertDuration(Duration.ofSeconds(Long.MAX_VALUE, 999999999L))
        assertDuration(Duration.ofSeconds(Long.MIN_VALUE, 0L))

        assertDuration(Duration.ofSeconds(-1234, 12341234L))
    }

    fun <T> assertConversion(converter: Converter<T>, value: T, column: String, verify: (Row) -> Unit = {}) {
        session.update("delete from $table")

        session.update("insert into $table ($column) values(:value)",
                mapOf("value" to converter.to(session.connection, value)))

        val actual = session.select("select $column from $table") { row -> verify(row); converter.from(row, column) }.single()
        println("expected=$value actual=$actual")
        assertEquals(value, actual)
    }
}