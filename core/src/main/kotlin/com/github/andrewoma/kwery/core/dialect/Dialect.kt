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

package com.github.andrewoma.kwery.core.dialect

import java.text.SimpleDateFormat
import java.sql.Blob

// TODO ... convert this to pass in a buffer for appending - improve performance in general
public trait Dialect {
    fun bind(value: Any): String

    fun bindArray(value: java.sql.Array, prefix: String = "", postfix: String = "") =
            (value.getArray() as Array<*>).stream().map {
                if (it == null) "null" else bind(it)
            }.joinToString(",", prefix, postfix)

    val supportsArrayBasedIn: Boolean

    fun arrayBasedIn(name: String): String {
        throw UnsupportedOperationException()
    }
}

val timestampFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("''yyyy-MM-dd HH:mm:ss.SSS''")
}

fun standardBlob(blob: Blob): String {
    val sb = StringBuilder("X'")
    sb.append(javax.xml.bind.DatatypeConverter.printHexBinary(blob.getBytes(1, blob.length().toInt())))
    sb.append("'")
    return sb.toString()
}

fun standardByteArray(bytes: ByteArray): String {
    val sb = StringBuilder("X'")
    sb.append(javax.xml.bind.DatatypeConverter.printHexBinary(bytes))
    sb.append("'")
    return sb.toString()
}

fun escapeSingleQuotedString(value: String): String {
    val sb = StringBuilder("'")
    for (char in value) {
        if (char == '\'') {
            sb.append('\'')
        }
        check(char != '\u0000', "Null characters are not permitted")
        sb.append(char)
    }
    sb.append("'")
    return sb.toString()
}