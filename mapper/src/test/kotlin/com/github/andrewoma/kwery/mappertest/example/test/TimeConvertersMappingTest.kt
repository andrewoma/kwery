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
import org.junit.Before
import org.junit.Test
import java.time.*
import kotlin.test.assertEquals

private val timeTableName = "time_mapping"

data class TimeTypes(
        val id: Int,
        val localDateTime: LocalDateTime,
        val localDate: LocalDate,
        val localTime: LocalTime,
        val instant: Instant,
        val duration: Duration,
        val offsetDateTime: OffsetDateTime,
        val zonedDateTime: ZonedDateTime
)

object timeTypesTable : Table<TimeTypes, Int>(timeTableName) {
    // @formatter:off
    val Id                by col(TimeTypes::id, id = true)
    val LocalDateTimeCol  by col(TimeTypes::localDateTime)
    val LocalDateCol      by col(TimeTypes::localDate)
    val LocalTimeCol      by col(TimeTypes::localTime)
    val InstantCol        by col(TimeTypes::instant)
    val DurationCol       by col(TimeTypes::duration)
    val OffsetDateTimeCol by col(TimeTypes::offsetDateTime)
    val ZonedDateTimeCol  by col(TimeTypes::zonedDateTime)
    // @formatter:on

    override fun idColumns(id: Int) = setOf(Id of id)

    override fun create(value: Value<TimeTypes>): TimeTypes = TimeTypes(
            value of Id,
            value of LocalDateTimeCol,
            value of LocalDateCol,
            value of LocalTimeCol,
            value of InstantCol,
            value of DurationCol,
            value of OffsetDateTimeCol,
            value of ZonedDateTimeCol
    )
}

class TimeTypesDao(session: Session) : AbstractDao<TimeTypes, Int>(session, timeTypesTable, { it.id }, null, IdStrategy.Explicit)

class TimeConvertersMappingTest : AbstractSessionTest() {

    @Before fun before() {
        initialise(this.javaClass.simpleName) {
            session.update("""
                create table $timeTableName(
                     id                   int,
                     local_date_time_col  timestamp,
                     local_date_col       date,
                     local_time_col       time,
                     instant_col          timestamp,
                     duration_col         decimal(28,9),
                     offset_date_time_col timestamp,
                     zoned_date_time_col  timestamp
                )
            """)
        }
    }

    @Test fun `should map time values`() {
        val dao = TimeTypesDao(session)
        val inserted = dao.insert(TimeTypes(
                1,
                LocalDateTime.now(),
                LocalDate.now(),
                LocalTime.now().withNano(0),
                Instant.now(),
                Duration.ofDays(22),
                OffsetDateTime.now(),
                ZonedDateTime.now()
        ))
        val fetched = dao.findById(1)
        assertEquals(inserted, fetched)
    }
}