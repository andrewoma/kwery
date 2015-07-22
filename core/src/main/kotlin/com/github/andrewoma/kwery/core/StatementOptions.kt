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

import java.sql.ResultSet
import java.sql.Statement

/**
 * StatementOptions allows configuration of JDBC statement options
 */
data class StatementOptions(
        /**
         * An optional name for the query for logging and monitoring.
         */
        val name: String? = null,

        /**
         * Applies the name to SQL as an inline comment for query logs.
         */
        val applyNameToQuery: Boolean = false,

        /**
         * If true, always use a prepared statement to execute statements. Not implemented yet - always used PreparedStatements
         */
        val usePreparedStatement: Boolean = true,

        /**
         * Sets the result set concurrency. Either `ResultSet.CONCUR_READ_ONLY` (default) or `ResultSet.CONCUR_UPDATABLE`
         */
        val resultSetConcurrency: Int = ResultSet.CONCUR_READ_ONLY,

        /**
         * Sets the result set type. One of `ResultSet.TYPE_FORWARD_ONLY` (default), `ResultSet.TYPE_SCROLL_INSENSITIVE`,
         * or `ResultSet.TYPE_SCROLL_SENSITIVE`
         */
        val resultSetType: Int = ResultSet.TYPE_FORWARD_ONLY,

        /**
         * Sets the result set type. One of `ResultSet.HOLD_CURSORS_OVER_COMMIT` or, `ResultSet.CLOSE_CURSORS_AT_COMMIT`.
         * Default (null) uses the connection's default holdability
         */
        val resultSetHoldability: Int? = null,

        /**
         * If true, use generated keys. Should be mutually exclusive of generatedKeyColumns
         */
        val useGeneratedKeys: Boolean = false,

        /**
         * The generated key columns. Should be mutually exclusive of useGeneratedKeys
         */
        val generatedKeyColumns: List<String> = listOf(),

        /**
         * Sets the fetch size hint. Default (0) means the hint is ignored.
         */
        val fetchSize: Int = 0,

        /**
         * Sets the query timeout in seconds. Default (0) means no limit.
         */
        val queryTimeout: Int = 0,

        /**
         * Sets the maximum number of bytes returned for large field types such a LOBs. Default (0) means no limit.
         */
        val maxFieldSize: Int = 0,

        /**
         * Sets the maximum number of rows returned. Default (0) means no limit.
         */
        val maxRows: Long = 0,

        /**
         * Sets hint as to whether the statement is poolable. Defaults to `usePreparedStatement`
         */
        val poolable: Boolean = usePreparedStatement,

        /**
         * Sets the fetch direction. One of given direction is not one of `ResultSet.FETCH_FORWARD`,
         * `ResultSet.FETCH_REVERSE`, or `ResultSet.FETCH_UNKNOWN`. Default is `ResultSet.FETCH_FORWARD`
         */
        val fetchDirection: Int = ResultSet.FETCH_FORWARD,

        /**
         * Invoked before a statement is executed allowing the setting of infrequently used or driver specific settings.
         */
        val beforeExecution: (Statement) -> Unit = {}
)