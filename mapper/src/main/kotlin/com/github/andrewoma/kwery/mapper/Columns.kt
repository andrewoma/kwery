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

public class PrefixedColumns<T : Any, ID>(val prefix: String, table: Table<T, ID>, columns: Set<Column<T, *>>) {
    val mapper: (Row) -> T = table.rowMapper(columns, { "${prefix}__${it.name}" })

    val optionalMapper: (Row) -> T? = { row ->
        val column = columns.firstOrNull { !it.isNullable }
        if (row.objectOrNull("${prefix}__${column!!.name}") == null) null else mapper(row)
    }

    val select: String = columns.asSequence().map { "${prefix}.${it.name} ${prefix}__${it.name}" }.joinToString(", ")
}

fun <T : Any, ID> Table<T, ID>.prefixed(prefix: String, columns: Set<Column<T, *>> = this.defaultColumns) = PrefixedColumns(prefix, this, columns)

fun <R1, R2> combine(mapper1: (Row) -> R1, mapper2: (Row) -> R2): (Row) -> Pair<R1, R2> {
    return { mapper1(it) to mapper2(it) }
}

fun <R1, R2, R3> combine(mapper1: (Row) -> R1, mapper2: (Row) -> R2, mapper3: (Row) -> R3): (Row) -> Triple<R1, R2, R3> {
    return { Triple(mapper1(it), mapper2(it), mapper3(it)) }
}