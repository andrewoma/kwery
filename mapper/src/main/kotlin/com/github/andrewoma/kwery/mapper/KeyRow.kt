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

package com.github.andrewoma.kwery.mapper

import com.github.andrewoma.kwery.core.Row
import java.sql.ResultSet

/**
 * A variant of row that fetches the generated key from the result set
 * by column index, not name
 */
class KeyRow(resultSet: ResultSet) : Row(resultSet) {
    override fun int(name: String) = resultSet.getInt(1)
    override fun intOrNull(name: String) = resultSet.getInt(1)
    override fun long(name: String) = resultSet.getLong(1)
    override fun longOrNull(name: String) = resultSet.getLong(1)

    override fun obj(name: String) = unsupported()
    override fun objectOrNull(name: String) = unsupported()
    override fun boolean(name: String) = unsupported()
    override fun booleanOrNull(name: String) = unsupported()
    override fun byte(name: String) = unsupported()
    override fun byteOrNull(name: String) = unsupported()
    override fun short(name: String) = unsupported()
    override fun shortOrNull(name: String) = unsupported()
    override fun float(name: String) = unsupported()
    override fun floatOrNull(name: String) = unsupported()
    override fun double(name: String) = unsupported()
    override fun doubleOrNull(name: String) = unsupported()
    override fun bigDecimal(name: String) = unsupported()
    override fun bigDecimalOrNull(name: String) = unsupported()
    override fun string(name: String) = unsupported()
    override fun stringOrNull(name: String) = unsupported()
    override fun bytes(name: String) = unsupported()
    override fun bytesOrNull(name: String) = unsupported()
    override fun timestamp(name: String) = unsupported()
    override fun timestampOrNull(name: String) = unsupported()
    override fun time(name: String) = unsupported()
    override fun timeOrNull(name: String) = unsupported()
    override fun date(name: String) = unsupported()
    override fun dateOrNull(name: String) = unsupported()
    override fun clob(name: String) = unsupported()
    override fun clobOrNull(name: String) = unsupported()
    override fun blob(name: String) = unsupported()
    override fun blobOrNull(name: String) = unsupported()
    override fun characterStream(name: String) = unsupported()
    override fun characterStreamOrNull(name: String) = unsupported()
    override fun binaryStream(name: String) = unsupported()
    override fun binaryStreamOrNull(name: String) = unsupported()

    private fun unsupported(): Nothing {
        throw UnsupportedOperationException("")
    }
}