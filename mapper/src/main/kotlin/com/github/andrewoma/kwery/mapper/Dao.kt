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


public trait Dao<T : Any, ID : Any> {
    public val defaultColumns: Set<Column<T, *>>
    public val defaultIdStrategy: IdStrategy

    public fun findById(id: ID, columns: Set<Column<T, *>> = defaultColumns): T?

    public fun findByIds(ids: Collection<ID>, columns: Set<Column<T, *>> = defaultColumns): Map<ID, T>

    public fun findAll(columns: Set<Column<T, *>> = defaultColumns): List<T>

    public fun findByExample(example: T, exampleColumns: Set<Column<T, *>>, columns: Set<Column<T, *>> = defaultColumns): List<T>

    public fun update(oldValue: T, newValue: T, deltaOnly: Boolean = false): T

    public fun delete(id: ID): Int

    public fun unsafeUpdate(newValue: T): Int

    public fun insert(value: T, idStrategy: IdStrategy = defaultIdStrategy): T

    public fun batchInsert(values: List<T>, idStrategy: IdStrategy = defaultIdStrategy): List<T>

    public fun unsafeBatchUpdate(values: List<T>)

    public fun batchUpdate(values: List<Pair<T, T>>): List<T>

    public fun allocateIds(count: Int): List<ID>
}