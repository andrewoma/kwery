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
         * Sets the result set concurrency. Defaults to ReadOnly
         */
        val resultSetConcurrency: ResultSetConcurrency = ResultSetConcurrency.ReadOnly,

        /**
         * Sets the result set type. Defaults to ForwardOnly
         */
        val resultSetType: ResultSetType = ResultSetType.ForwardOnly,

        /**
         * Sets the result set type. Defaults to ConnectionDefault
         */
        val resultSetHoldability: ResultSetHoldability = ResultSetHoldability.ConnectionDefault,

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
         * This sets the max rows at the JDBC driver level. Whether this limits client or server side is
         * up to the specific JDBC driver. See `limit` for a server side solution.
         */
        val maxRows: Long = 0,

        /**
         * Sets hint as to whether the statement is poolable. Defaults to `usePreparedStatement`
         */
        val poolable: Boolean = usePreparedStatement,

        /**
         * Sets the fetch direction. Defaults to Forward
         */
        val fetchDirection: ResultSetFetchDirection = ResultSetFetchDirection.Forward,

        /**
         * Invoked before a statement is executed allowing the setting of infrequently used or driver specific settings.
         */
        val beforeExecution: (Statement) -> Unit = {},

        /**
         * Limits the number of rows returned server side by modifying the query to include a
         * dialect specific limit
         */
        val limit: Int? = null,

        /**
         * Skips to the offset specified server side by modifying the query to include a
         * dialect specific limit
         */
        val offset: Int? = null
)

enum class ResultSetConcurrency constructor(val value: Int) {
    ReadOnly(ResultSet.CONCUR_READ_ONLY),
    Updatable(ResultSet.CONCUR_UPDATABLE);
}

enum class ResultSetType constructor(val value: Int) {
    ForwardOnly(ResultSet.TYPE_FORWARD_ONLY),
    ScrollInsensitive(ResultSet.TYPE_SCROLL_INSENSITIVE),
    ScrollSensitive(ResultSet.TYPE_SCROLL_SENSITIVE);
}

enum class ResultSetHoldability constructor(val value: Int?) {
    HoldCursorsOverCommit(ResultSet.HOLD_CURSORS_OVER_COMMIT),
    CloseCursorsOverCommit(ResultSet.CLOSE_CURSORS_AT_COMMIT),
    ConnectionDefault(null);
}

enum class ResultSetFetchDirection constructor(val value: Int) {
    Forward(ResultSet.FETCH_FORWARD),
    Reverse(ResultSet.FETCH_REVERSE);
}
