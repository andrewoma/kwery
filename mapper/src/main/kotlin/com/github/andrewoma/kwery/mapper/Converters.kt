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

import com.github.andrewoma.kwery.core.Row
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

public open class Converter<R>(
        public val from: (Row, String) -> R,
        public val to: (Connection, R) -> Any?
)

public abstract class SimpleConverter<R>(from: (Row, String) -> R, to: (R) -> Any? = { it }) : Converter<R>(from, { c, v -> to(v) })

public val standardConverters: Map<Class<*>, Converter<*>> = listOf(
        reifiedConverter(booleanConverter),
        reifiedConverter(byteConverter),
        reifiedConverter(shortConverter),
        reifiedConverter(intConverter),
        reifiedConverter(longConverter),
        reifiedConverter(floatConverter),
        reifiedConverter(doubleConverter),
        reifiedConverter(bigDecimalConverter),
        reifiedConverter(stringConverter),
        reifiedConverter(bytesConverter),
        reifiedConverter(timestampConverter),
        reifiedConverter(timeConverter),
        reifiedConverter(dateConverter)
).toMap()

public val timeConverters: Map<Class<*>, Converter<*>> = listOf(
        reifiedConverter(localDateTimeConverter)
).toMap()

public inline fun <reified T> reifiedConverter(converter: Converter<T>): Pair<Class<T>, Converter<T>> = javaClass<T>() to converter

public object localDateTimeConverter : SimpleConverter<LocalDateTime>(
        { row, c -> LocalDateTime.ofInstant(row.timestamp(c).toInstant(), ZoneId.systemDefault()) },
        { Timestamp.from(it.atZone(ZoneId.systemDefault()).toInstant()) }
)

public inline fun <reified T> ArrayConverter(sqlType: String): Converter<List<T>> {
    return Converter(
            { row, c -> row.array<T>(c).toList() },
            { c, v -> c.createArrayOf(sqlType, v.copyToArray()) }
    )
}

public object booleanConverter : SimpleConverter<Boolean>({ row, c -> row.boolean(c) })

public object byteConverter : SimpleConverter<Byte>({ row, c -> row.byte(c) })

public object shortConverter : SimpleConverter<Short>({ row, c -> row.short(c) })

public object intConverter : SimpleConverter<Int>({ row, c -> row.int(c) })

public object longConverter : SimpleConverter<Long>({ row, c -> row.long(c) })

public object floatConverter : SimpleConverter<Float>({ row, c -> row.float(c) })

public object doubleConverter : SimpleConverter<Double>({ row, c -> row.double(c) })

public object bigDecimalConverter : SimpleConverter<BigDecimal>({ row, c -> row.bigDecimal(c) })

public object stringConverter : SimpleConverter<String>({ row, c -> row.string(c) })

public object bytesConverter : SimpleConverter<ByteArray>({ row, c -> row.bytes(c) })

public object timestampConverter : SimpleConverter<Timestamp>({ row, c -> row.timestamp(c) })

public object timeConverter : SimpleConverter<Time>({ row, c -> row.time(c) })

public object dateConverter : SimpleConverter<Date>({ row, c -> row.date(c) })

public object clobConverter : Converter<String>(
        { row, c -> row.clob(c).let { it.getSubString(1, it.length().toInt()) } },
        { c, v -> c.createClob().let { it.setString(1, v); it } }
)

public object blobConverter : Converter<ByteArray>(
        { row, c -> row.blob(c).let { it.getBytes(1, it.length().toInt()) } },
        { c, v -> c.createBlob().let { it.setBytes(1, v); it } }
)

public fun <R : Any> optional(converter: Converter<R>): Converter<R?> = Converter(
        { row, c -> if (row.objectOrNull(c) == null) null else converter.from(row, c) },
        { c, v -> if (v == null) null else converter.to(c, v) }
)

public class DurationConverter(unit: TemporalUnit) : SimpleConverter<Duration>(
        { row, c -> Duration.of(row.long(c), unit) },
        {
            when (unit) { // Rooted Java API (again!)
                ChronoUnit.NANOS -> it.toNanos()
                ChronoUnit.MICROS -> it.toNanos() / 1000
                ChronoUnit.MILLIS -> it.toMillis()
                ChronoUnit.SECONDS -> it.getSeconds()
                ChronoUnit.MINUTES -> it.toMinutes()
                ChronoUnit.HOURS -> it.toHours()
                ChronoUnit.HALF_DAYS -> it.toDays() * 2
                ChronoUnit.DAYS -> it.toDays()
                ChronoUnit.WEEKS -> it.toDays() / 7

                else -> throw UnsupportedOperationException()
            }
        }
)

public class EnumByNameConverter<T : Enum<T>>(type: Class<T>) : SimpleConverter<T>(
        { row, c -> java.lang.Enum.valueOf(type, row.string(c)) },
        { it.name() }
)
