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

import java.sql.Blob
import java.sql.Clob
import java.text.SimpleDateFormat

// TODO ... convert this to pass in a buffer for appending - improve performance in general
interface Dialect {
    fun bind(value: Any, limit: Int): String

    fun bindArray(value: java.sql.Array, limit: Int, prefix: String = "", postfix: String = "") =
            (value.array as Array<*>).asSequence().map {
                if (it == null) "null" else bind(it, limit)
            }.joinToString(",", prefix, postfix)

    val supportsArrayBasedIn: Boolean
    val supportsAllocateIds: Boolean

    fun arrayBasedIn(name: String): String

    fun allocateIds(count: Int, sequence: String, columnName: String): String
}

internal fun String.truncate(limit: Int): String {
    return if (limit == -1 || limit >= this.length) this else this.take(limit)
}

internal fun ByteArray.truncate(limit: Int): ByteArray {
    return if (limit == -1 || limit >= this.size) this else {
        val result = ByteArray(limit)
        System.arraycopy(this, 0, result, 0, limit)
        result
    }
}

internal val timestampFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("''yyyy-MM-dd HH:mm:ss.SSS''")
}

internal fun standardBlob(blob: Blob, limit: Int): String {
    val length = if (limit == -1) blob.length().toInt() else Math.min(limit, blob.length().toInt())
    return standardByteArray(blob.getBytes(1, length), limit)
}

internal fun standardClob(clob: Clob, limit: Int): String {
    val length = if (limit == -1) clob.length().toInt() else Math.min(limit, clob.length().toInt())
    return clob.getSubString(1, length)
}

internal fun standardByteArray(bytes: ByteArray, limit: Int): String {
    val sb = StringBuilder("X'")
    sb.append(javax.xml.bind.DatatypeConverter.printHexBinary(bytes.truncate(limit)))
    sb.append("'")
    return sb.toString()
}

internal fun escapeSingleQuotedString(value: String): String {
    val sb = StringBuilder("'")
    for (char in value) {
        if (char == '\'') {
            sb.append('\'')
        }
        check(char != '\u0000') { "Null characters are not permitted" }
        sb.append(char)
    }
    sb.append("'")
    return sb.toString()
}