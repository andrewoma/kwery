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

import java.sql.Timestamp
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.Time

public open class HsqlDialect : Dialect {

    override fun bind(value: Any): String = when (value) {
        is String -> escapeSingleQuotedString(value)
        is Timestamp -> timestampFormat.get().format(value)
        is Date -> "'$value'"
        is Time -> "'$value'"
        is java.sql.Array -> bindArray(value, "ARRAY[", "]")
        is Blob -> standardBlob(value)
        is ByteArray -> standardByteArray(value)
        is Clob -> escapeSingleQuotedString(value.getSubString(1, value.length().toInt()))
        else -> value.toString()
    }
}