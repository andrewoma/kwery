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

import com.github.andrewoma.kwery.core.dialect.Dialect
import java.sql.Connection

/**
 * Session is the key interface for querying the database in kwery.
 *
 * Sessions hold an underlying JDBC connection.
 */
interface Session {
    /**
     * The current transaction for the session if a transaction has been started
     */
    public val currentTransaction: Transaction?

    /**
     * The underlying JDBC connection
     */
    public val connection: Connection

    public val dialect: Dialect

    /**
     * The default StatementOptions used for this session unless overridden explicitly on calls
     */
    val defaultOptions: StatementOptions

    /**
     * Executes a query returning the results as `List`
     */
    public fun <R> select(sql: String,
                          parameters: Map<String, Any?> = mapOf(),
                          options: StatementOptions = defaultOptions,
                          mapper: (Row) -> R): List<R>

    /**
     * Executes an update returning the count of rows affected by the statement
     */
    public fun update(sql: String,
                      parameters: Map<String, Any?> = mapOf(),
                      options: StatementOptions = defaultOptions): Int

    /**
     * Executes an insert statement with generated keys, returning the keys
     */
    public fun <K> insert(sql: String,
                          parameters: Map<String, Any?> = mapOf(),
                          options: StatementOptions = defaultOptions, f: (Row) -> K): Pair<Int, K>

    /**
     * Executes a batch of update statements returning the counts of each statement executed
     */
    public fun batchUpdate(sql: String,
                           parametersList: List<Map<String, Any?>>,
                           options: StatementOptions = defaultOptions): List<Int>

    /**
     * Executes a batch of insert statements with generated keys, returning the list of keys
     */
    public fun <K> batchInsert(sql: String,
                               parametersList: List<Map<String, Any?>>,
                               options: StatementOptions = defaultOptions,
                               f: (Row) -> K): List<Pair<Int, K>>

    /**
     * Executes a query, providing the results as a sequence for streaming.
     * This is the most flexible method for handling large result sets without loading them into memory.
     */
    public fun <R> asSequence(sql: String,
                            parameters: Map<String, Any?> = mapOf(),
                            options: StatementOptions = defaultOptions,
                            f: (Sequence<Row>) -> R): R

    /**
     * Executes a query, invoking the supplied function for each row returned.
     * This is suitable for handling large result sets without loading them into memory.
     */
    public fun forEach(sql: String,
                       parameters: Map<String, Any?> = mapOf(),
                       options: StatementOptions = defaultOptions,
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

    /**
     * Starts a transaction for the lifetime of the supplied function.
     * The transaction will be committed automatically unless an exception is thrown or `transaction.rollbackOnly`
     * is set to true
     */
    public fun <R> transaction(f: (Transaction) -> R): R

    /**
     * Starts a transaction, allowing manual control over whether the transaction is committed or rolled back.
     * The use of this method is discouraged and is intended for use by framework code - use the `transaction`
     * method instead where possible.
     */
    public fun manualTransaction(): ManualTransaction
}
