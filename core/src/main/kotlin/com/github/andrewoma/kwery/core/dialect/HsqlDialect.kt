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

import java.sql.*

public open class HsqlDialect : Dialect {

    override fun bind(value: Any, limit: Int): String = when (value) {
        is String -> escapeSingleQuotedString(value.truncate(limit))
        is Timestamp -> timestampFormat.get().format(value)
        is Date -> "'$value'"
        is Time -> "'$value'"
        is java.sql.Array -> bindArray(value, limit, "array[", "]")
        is Blob -> standardBlob(value, limit)
        is ByteArray -> standardByteArray(value, limit)
        is Clob -> escapeSingleQuotedString(standardClob(value, limit))
        else -> value.toString()
    }

    override val supportsArrayBasedIn = true

    override fun arrayBasedIn(name: String) = "in(unnest(:$name))"

    override val supportsAllocateIds = true

    override fun allocateIds(count: Int, sequence: String, columnName: String) =
            "select next value for $sequence as $columnName from unnest(sequence_array(1, $count, 1))"
}