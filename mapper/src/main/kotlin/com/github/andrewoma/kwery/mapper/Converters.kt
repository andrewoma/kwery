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

open class Converter<R>(
        val from: (Row, String) -> R,
        val to: (Connection, R) -> Any?
)

abstract class SimpleConverter<R>(from: (Row, String) -> R, to: (R) -> Any? = { it }) : Converter<R>(from, { c, v -> to(v) })

val standardConverters: Map<Class<*>, Converter<*>> = listOf(
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
).toMap() + mapOf<Class<*>, Converter<*>>(
        java.lang.Boolean.TYPE to booleanConverter,
        java.lang.Byte.TYPE to byteConverter,
        java.lang.Short.TYPE to shortConverter,
        java.lang.Integer.TYPE to intConverter,
        java.lang.Long.TYPE to longConverter,
        java.lang.Float.TYPE to floatConverter,
        java.lang.Double.TYPE to doubleConverter
)

inline fun <reified T : Any> reifiedConverter(converter: Converter<T>): Pair<Class<*>, Converter<T>> {
    return T::class.java to converter
}

inline fun <reified T> ArrayConverter(sqlType: String): Converter<List<T>> {
    return Converter(
            { row, c -> row.array<T>(c).toList() },
            { c, v -> c.createArrayOf(sqlType, v.toTypedArray()) }
    )
}

object booleanConverter : SimpleConverter<Boolean>({ row, c -> row.boolean(c) })

object byteConverter : SimpleConverter<Byte>({ row, c -> row.byte(c) })

object shortConverter : SimpleConverter<Short>({ row, c -> row.short(c) })

object intConverter : SimpleConverter<Int>({ row, c -> row.int(c) })

object longConverter : SimpleConverter<Long>({ row, c -> row.long(c) })

object floatConverter : SimpleConverter<Float>({ row, c -> row.float(c) })

object doubleConverter : SimpleConverter<Double>({ row, c -> row.double(c) })

object bigDecimalConverter : SimpleConverter<BigDecimal>({ row, c -> row.bigDecimal(c) })

object stringConverter : SimpleConverter<String>({ row, c -> row.string(c) })

object bytesConverter : SimpleConverter<ByteArray>({ row, c -> row.bytes(c) })

object timestampConverter : SimpleConverter<Timestamp>({ row, c -> row.timestamp(c) })

object timeConverter : SimpleConverter<Time>({ row, c -> row.time(c) })

object dateConverter : SimpleConverter<Date>({ row, c -> row.date(c) })

object clobConverter : Converter<String>(
        { row, c -> row.clob(c).let { it.getSubString(1, it.length().toInt()) } },
        { c, v -> c.createClob().let { it.setString(1, v); it } }
)

object blobConverter : Converter<ByteArray>(
        { row, c -> row.blob(c).let { it.getBytes(1, it.length().toInt()) } },
        { c, v -> c.createBlob().let { it.setBytes(1, v); it } }
)

fun <R : Any> optional(converter: Converter<R>): Converter<R?> = Converter(
        { row, c -> if (row.objectOrNull(c) == null) null else converter.from(row, c) },
        { c, v -> if (v == null) null else converter.to(c, v) }
)

class EnumByNameConverter<T : Enum<T>>(type: Class<T>) : SimpleConverter<T>(
        { row, c -> java.lang.Enum.valueOf(type, row.string(c)) },
        { it.name() }
)
