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

val offsetDateTimeConverter = OffsetDateTimeConverter()
val zonedDateTimeConverter = ZonedDateTimeConverter()

val timeConverters: Map<Class<*>, Converter<*>> = listOf(
        reifiedConverter(localDateTimeConverter),
        reifiedConverter(localDateConverter),
        reifiedConverter(localTimeConverter),
        reifiedConverter(instantConverter),
        reifiedConverter(durationToBigDecimalConverter),
        reifiedConverter(offsetDateTimeConverter),
        reifiedConverter(zonedDateTimeConverter)
).toMap()

object localDateTimeConverter : SimpleConverter<LocalDateTime>(
        { row, c -> LocalDateTime.ofInstant(row.timestamp(c).toInstant(), ZoneId.systemDefault()) },
        { Timestamp.from(it.atZone(ZoneId.systemDefault()).toInstant()) }
)

object localDateConverter : SimpleConverter<LocalDate>(
        { row, c -> row.date(c).toLocalDate() },
        { Date.valueOf(it) }
)

object localTimeConverter : SimpleConverter<LocalTime>(
        { row, c -> row.time(c).toLocalTime() },
        {
            @Suppress("DEPRECATION") // No sane alternative
            Time(it.hour, it.minute, it.second)
        }
)

object instantConverter : SimpleConverter<Instant>(
        { row, c -> row.timestamp(c).toInstant() },
        { Timestamp(it.toEpochMilli()) }
)

/**
 * Note: the offset is not stored in the database and will be lost. When reading the offset will
 * be set based on the zone id given to the converter on construction.
 */
class OffsetDateTimeConverter(val zone: ZoneId = ZoneId.systemDefault()) : SimpleConverter<OffsetDateTime>(
        { row, c -> OffsetDateTime.ofInstant(row.timestamp(c).toInstant(), zone)},
        { Timestamp(it.toInstant().toEpochMilli()) }
)

/**
 * Note: the zone is not stored in the database and will be lost. When reading the offset will
 * be set based on the zone id given to the converter on construction.
 */
class ZonedDateTimeConverter(val zone: ZoneId = ZoneId.systemDefault()) : SimpleConverter<ZonedDateTime>(
        { row, c -> ZonedDateTime.ofInstant(row.timestamp(c).toInstant(), zone)},
        { Timestamp(it.toInstant().toEpochMilli()) }
)

/**
 * Converts a Duration to a BigDecimal of seconds for storage.
 * A duration's max value is 9,223,372,036,854,775,807.999999999 seconds so requires DECIMAL(28,9) to store all
 * durations without overflow or truncation.
 */
object durationToBigDecimalConverter : SimpleConverter<Duration>(
        { row, c -> row.bigDecimal(c).toDuration() },
        { it.toBigDecimal() }
)

/**
 * Converts a duration to a given unit when storing in the database as an integer.
 * Only NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS and DAYS are supported.
 * Ensure that if converting between units for storage that the conversion is a whole number
 * of the unit specified and does not overflow a Long.
 */
class DurationConverter(unit: TemporalUnit) : SimpleConverter<Duration>(
        { row, c -> Duration.of(row.long(c), unit) },
        { duration ->
            val converted = when (unit) {
                NANOS -> duration.toNanos()
                MICROS -> duration.toNanos() / 1000
                MILLIS -> duration.toMillis()
                SECONDS -> duration.seconds
                MINUTES -> duration.toMinutes()
                HOURS -> duration.toHours()
                HALF_DAYS -> duration.toHours() / 12
                DAYS -> duration.toDays()
                else -> throw UnsupportedOperationException("") // Not possible
            }
            require(duration == Duration.of(converted, unit)) { "$duration must be a whole number of $unit and not overflow a Long" }
            converted
        }
) {
    companion object {
        private val supported = setOf(NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS)
    }

    init {
        require(supported.containsRaw(unit)) { "Only ${supported.joinToString(", ")} are supported" }
    }
}

private val nanosInSecond: BigDecimal = BigDecimal.valueOf(1000000000L)

fun BigDecimal.toDuration(): Duration {
    val seconds = this.toLong()
    val nanoseconds = this.remainder(BigDecimal.ONE).multiply(nanosInSecond).toLong()
    return Duration.ofSeconds(seconds, nanoseconds);
}

fun Duration.toBigDecimal(): BigDecimal {
    return BigDecimal.valueOf(this.seconds).add(BigDecimal.valueOf(this.nano.toLong()).divide(nanosInSecond))
}