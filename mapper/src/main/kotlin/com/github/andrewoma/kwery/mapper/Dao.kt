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


interface Dao<T : Any, ID : Any> {
    val defaultColumns: Set<Column<T, *>>
    val defaultIdStrategy: IdStrategy

    fun findById(id: ID, columns: Set<Column<T, *>> = defaultColumns): T?

    fun findByIds(ids: Collection<ID>, columns: Set<Column<T, *>> = defaultColumns): Map<ID, T>

    fun findAll(columns: Set<Column<T, *>> = defaultColumns): List<T>

    fun findByExample(example: T, exampleColumns: Set<Column<T, *>>, columns: Set<Column<T, *>> = defaultColumns): List<T>

    fun update(oldValue: T, newValue: T, deltaOnly: Boolean = false): T

    fun delete(id: ID): Int

    fun unsafeUpdate(newValue: T): Int

    fun insert(value: T, idStrategy: IdStrategy = defaultIdStrategy): T

    fun batchInsert(values: List<T>, idStrategy: IdStrategy = defaultIdStrategy): List<T>

    fun unsafeBatchUpdate(values: List<T>)

    fun batchUpdate(values: List<Pair<T, T>>): List<T>

    fun allocateIds(count: Int): List<ID>
}