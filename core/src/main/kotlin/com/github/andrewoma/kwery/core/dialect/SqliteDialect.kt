/*
 * Copyright (c) 2016 Andrew O'Malley
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

import java.sql.*

open class SqliteDialect : Dialect {

    override fun bind(value: Any, limit: Int) = when (value) {
        is String -> escapeSingleQuotedString(value.truncate(limit))
        is Timestamp -> timestampFormat.get().format(value)
        is Date -> "'$value 00:00:00.000'"
        is Time -> "'1970-01-01 $value.000'"
        is java.sql.Array -> throw UnsupportedOperationException()
        is Clob -> escapeSingleQuotedString(standardClob(value, limit))
        is Blob -> standardBlob(value, limit)
        is ByteArray -> standardByteArray(value, limit)
        else -> value.toString()
    }

    override fun arrayBasedIn(name: String) = throw UnsupportedOperationException()

    override val supportsArrayBasedIn = false

    override val supportsAllocateIds = false

    override fun allocateIds(count: Int, sequence: String, columnName: String) = throw UnsupportedOperationException()

    override val supportsFetchingGeneratedKeysByName = false
}
