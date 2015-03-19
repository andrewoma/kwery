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

import java.sql.Connection
import com.github.andrewoma.kwery.core.dialect.Dialect

trait Session {
    public val currentTransaction: Transaction?

    public val connection: Connection

    public val dialect: Dialect

    val defaultSelectOptions: SelectOptions

    val defaultUpdateOptions: UpdateOptions

    public fun <R> select(sql: String,
                          parameters: Map<String, Any?> = mapOf(),
                          options: SelectOptions = defaultSelectOptions,
                          mapper: (Row) -> R): List<R>

    public fun update(sql: String,
                      parameters: Map<String, Any?> = mapOf(),
                      options: UpdateOptions = defaultUpdateOptions): Int

    public fun <K> insert(sql: String,
                          parameters: Map<String, Any?> = mapOf(),
                          options: UpdateOptions = defaultUpdateOptions, f: (Row) -> K): Pair<Int, K>

    public fun batchUpdate(sql: String,
                           parametersList: List<Map<String, Any?>>,
                           options: UpdateOptions = defaultUpdateOptions): List<Int>

    public fun <K> batchInsert(sql: String,
                               parametersList: List<Map<String, Any?>>,
                               options: UpdateOptions = defaultUpdateOptions,
                               f: (Row) -> K): List<Pair<Int, K>>

    public fun <R> sequence(sql: String,
                      parameters: Map<String, Any?> = mapOf(),
                      options: SelectOptions = defaultSelectOptions,
                      f: (Sequence<Row>) -> R): R

    public fun forEach(sql: String,
                      parameters: Map<String, Any?> = mapOf(),
                      options: SelectOptions = defaultSelectOptions,
                      f: (Row) -> Unit): Unit

    /**
     * Binds parameters into a static SQL string.
     *
     * This can be used for logging, or (in the future) for direct execution bypassing prepared statements.
     *
     * Be careful not to introduce SQL injections if binding strings. The dialect will attempt to escape
     * strings so they are safe, but it is probably not reliable for untrusted strings.
     */
    public fun bindParameters(sql: String,
                              parameters: Map<String, Any?>,
                              closeParameters: Boolean = true,
                              limit: Int = -1,
                              consumeStreams: Boolean = true): String

    public fun <R> transaction(f: (Transaction) -> R): R

    public fun manualTransaction(): ManualTransaction
}
