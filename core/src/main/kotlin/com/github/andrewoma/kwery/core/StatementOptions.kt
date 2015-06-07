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

import java.sql.Statement

abstract class StatementOptions(
        /**
         * An optional name for the query for logging and monitoring.
         */
        val name: String? = null,

        /**
         * Applies the name to SQL as an inline comment for query logs. (TODO - implement)
         */
        val applyNameToQuery: Boolean = false,

        /**
         * If true, always use a prepared statement to execute statements. (TODO - PreparedStatements are always used)
         */
        val usePreparedStatement: Boolean = true,

        /**
         * Invoked before a statement is executed allowing the setting of infrequently used settings such as fetch direction.
         */
        val beforeExecution: (Statement) -> Unit = {}
) {
    abstract val cacheKey: Any?
}

open class SelectOptions(
        name: String? = null,
        applyNameToQuery: Boolean = false,
        usePreparedStatement: Boolean = true,
        beforeExecution: (Statement) -> Unit = {}
) : StatementOptions(name, applyNameToQuery, usePreparedStatement, beforeExecution) {

    override val cacheKey = listOf(name, applyNameToQuery)

    fun copy(name: String? = this.name,
             applyNameToQuery: Boolean = this.applyNameToQuery,
             usePreparedStatement: Boolean = this.usePreparedStatement,
             beforeExecution: (Statement) -> Unit = this.beforeExecution
    ): SelectOptions = SelectOptions(name, applyNameToQuery, usePreparedStatement, beforeExecution)
}

open class UpdateOptions(
        name: String? = null,
        applyNameToQuery: Boolean = false,
        usePreparedStatement: Boolean = true,
        beforeExecution: (Statement) -> Unit = {},
        val useGeneratedKeys: Boolean = false
) : StatementOptions(name, applyNameToQuery, usePreparedStatement, beforeExecution) {

    override val cacheKey = name

    fun copy(name: String? = this.name,
             applyNameToQuery: Boolean = this.applyNameToQuery,
             usePreparedStatement: Boolean = this.usePreparedStatement,
             beforeExecution: (Statement) -> Unit = this.beforeExecution,
             useGeneratedKeys: Boolean = this.useGeneratedKeys
    ): UpdateOptions = UpdateOptions(name, applyNameToQuery, usePreparedStatement, beforeExecution, useGeneratedKeys)
}
