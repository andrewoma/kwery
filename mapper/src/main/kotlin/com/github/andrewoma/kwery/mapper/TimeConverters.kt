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

package com.github.andrewoma.kwery.mapper

import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.time.temporal.ChronoUnit.*
import java.time.temporal.TemporalUnit

public val timeConverters: Map<Class<*>, Converter<*>> = listOf(
        reifiedConverter(localDateTimeConverter),
        reifiedConverter(localDateConverter),
        reifiedConverter(localTimeConverter),
        reifiedConverter(instantConverter),
        reifiedConverter(durationToBigDecimalConverter)
).toMap()

public object localDateTimeConverter : SimpleConverter<LocalDateTime>(
        { row, c -> LocalDateTime.ofInstant(row.timestamp(c).toInstant(), ZoneId.systemDefault()) },
        { Timestamp.from(it.atZone(ZoneId.systemDefault()).toInstant()) }
)

public object localDateConverter : SimpleConverter<LocalDate>(
        { row, c -> row.date(c).toLocalDate() },
        { Date.valueOf(it) }
)

public object localTimeConverter : SimpleConverter<LocalTime>(
        { row, c -> row.time(c).toLocalTime() },
        {
            @suppress("DEPRECATED_SYMBOL_WITH_MESSAGE") // No sane alternative
            Time(it.getHour(), it.getMinute(), it.getSecond())
        }
)

public object instantConverter : SimpleConverter<Instant>(
        { row, c -> row.timestamp(c).toInstant() },
        { Timestamp(it.toEpochMilli()) }
)

/**
 * Converts a Duration to a BigDecimal of seconds for storage.
 * A duration's max value is 9,223,372,036,854,775,807.999999999 seconds so requires DECIMAL(28,9) to store all
 * durations without overflow or truncation.
 */
public object durationToBigDecimalConverter : SimpleConverter<Duration>(
        { row, c -> row.bigDecimal(c).toDuration() },
        { it.toBigDecimal() }
)

/**
 * Converts a duration to a given unit when storing in the database as an integer.
 * Only NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS and DAYS are supported.
 * Ensure that if converting between units for storage that the conversion is a whole number
 * of the unit specified and does not overflow a Long.
 */
public class DurationConverter(unit: TemporalUnit) : SimpleConverter<Duration>(
        { row, c -> Duration.of(row.long(c), unit) },
        { duration ->
            val converted = when (unit) {
                NANOS -> duration.toNanos()
                MICROS -> duration.toNanos() / 1000
                MILLIS -> duration.toMillis()
                SECONDS -> duration.getSeconds()
                MINUTES -> duration.toMinutes()
                HOURS -> duration.toHours()
                HALF_DAYS -> duration.toHours() / 12
                DAYS -> duration.toDays()
                else -> throw UnsupportedOperationException("") // Not possible
            }
            require(duration == Duration.of(converted, unit), "${duration} must be a whole number of ${unit} and not overflow a Long")
            converted
        }
) {
    companion object {
        val supported = setOf(NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS)
    }

    init {
        require(unit in supported, "Only ${supported.joinToString(", ")} are supported")
    }
}

private val nanosInSecond: BigDecimal = BigDecimal.valueOf(1000000000L)

public fun BigDecimal.toDuration(): Duration {
    val seconds = this.longValue()
    val nanoseconds = this.remainder(BigDecimal.ONE).multiply(nanosInSecond).longValue()
    return Duration.ofSeconds(seconds, nanoseconds);
}

public fun Duration.toBigDecimal(): BigDecimal {
    return BigDecimal.valueOf(this.getSeconds()).add(BigDecimal.valueOf(this.getNano().toLong()).divide(nanosInSecond))
}