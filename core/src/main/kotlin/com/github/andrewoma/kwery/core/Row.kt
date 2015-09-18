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

import java.io.InputStream
import java.io.Reader
import java.math.BigDecimal
import java.sql.*

/**
 * Row is a thin wrapper over the JDBC ResultSet to provide clean and consistent
 * null handling.
 */
public class Row(val resultSet: ResultSet) {
    public fun obj(name: String): Any = requireNotNull(resultSet.getObject(name), name)
    public fun objectOrNull(name: String): Any? = resultSet.getObject(name)

    public fun boolean(name: String): Boolean = requireNotNull(resultSet.getBoolean(name), name)
    public fun booleanOrNull(name: String): Boolean? = valueOrNull(resultSet.getBoolean(name))

    public fun byte(name: String): Byte = requireNotNull(resultSet.getByte(name), name)
    public fun byteOrNull(name: String): Byte? = valueOrNull(resultSet.getByte(name))

    public fun short(name: String): Short = requireNotNull(resultSet.getShort(name), name)
    public fun shortOrNull(name: String): Short? = valueOrNull(resultSet.getShort(name))

    public fun int(name: String): Int = requireNotNull(resultSet.getInt(name), name)
    public fun intOrNull(name: String): Int? = valueOrNull(resultSet.getInt(name))

    public fun long(name: String): Long = requireNotNull(resultSet.getLong(name), name)
    public fun longOrNull(name: String): Long? = valueOrNull(resultSet.getLong(name))

    public fun float(name: String): Float = requireNotNull(resultSet.getFloat(name), name)
    public fun floatOrNull(name: String): Float? = valueOrNull(resultSet.getFloat(name))

    public fun double(name: String): Double = requireNotNull(resultSet.getDouble(name), name)
    public fun doubleOrNull(name: String): Double? = valueOrNull(resultSet.getDouble(name))

    public fun bigDecimal(name: String): BigDecimal = resultSet.getBigDecimal(name)
    public fun bigDecimalOrNull(name: String): BigDecimal? = resultSet.getBigDecimal(name)

    public fun string(name: String): String = resultSet.getString(name)
    public fun stringOrNull(name: String): String? = resultSet.getString(name)

    public fun bytes(name: String): ByteArray = resultSet.getBytes(name)
    public fun bytesOrNull(name: String): ByteArray? = resultSet.getBytes(name)

    public fun timestamp(name: String): Timestamp = resultSet.getTimestamp(name)
    public fun timestampOrNull(name: String): Timestamp? = resultSet.getTimestamp(name)

    public fun time(name: String): Time = resultSet.getTime(name)
    public fun timeOrNull(name: String): Time? = resultSet.getTime(name)

    public fun date(name: String): Date = resultSet.getDate(name)
    public fun dateOrNull(name: String): Date? = resultSet.getDate(name)

    public fun clob(name: String): Clob = resultSet.getClob(name)
    public fun clobOrNull(name: String): Clob? = resultSet.getClob(name)

    public fun blob(name: String): Blob = resultSet.getBlob(name)
    public fun blobOrNull(name: String): Blob? = resultSet.getBlob(name)

    public fun characterStream(name: String): Reader = resultSet.getCharacterStream(name)
    public fun characterStreamOrNull(name: String): Reader? = resultSet.getCharacterStream(name)

    public fun binaryStream(name: String): InputStream = resultSet.getBinaryStream(name)
    public fun binaryStreamOrNull(name: String): InputStream? = resultSet.getBinaryStream(name)

    @Suppress("UNCHECKED_CAST")
    public fun <T> array(name: String): List<T> {
        val value = resultSet.getArray(name)
        return if (resultSet.wasNull()) listOf() else (value.array as Array<Any>).toList() as List<T>
    }

    private fun <T : Any> valueOrNull(value: T): T? = if (resultSet.wasNull()) null else value

    private fun <T : Any> requireNotNull(value: T?, name: String): T {
        require(!resultSet.wasNull()) { "Unexpected null for column '$name'" }
        return value!!
    }
}
