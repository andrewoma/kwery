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

abstract class StatementOptions(
        val name: String? = null,
        val applyNameToQuery: Boolean = false,
        val closeParameters: Boolean = true,
        val usePreparedStatement: Boolean = true
) {
    abstract val cacheKey: Any?
}

open class SelectOptions(
        name: String? = null,
        applyNameToQuery: Boolean = false,
        closeParameters: Boolean = true,
        usePreparedStatement: Boolean = true
) : StatementOptions(name, applyNameToQuery, closeParameters, usePreparedStatement) {

    override val cacheKey = listOf(name, applyNameToQuery)

    fun copy(name: String? = this.name,
             applyNameToQuery: Boolean = this.applyNameToQuery,
             usePreparedStatement: Boolean = this.usePreparedStatement,
             closeParameters: Boolean = this.closeParameters
    ): SelectOptions = SelectOptions(name, applyNameToQuery, closeParameters, usePreparedStatement)
}

open class UpdateOptions(
        name: String? = null,
        applyNameToQuery: Boolean = false,
        usePreparedStatement: Boolean = true,
        closeParameters: Boolean = true,
        val useGeneratedKeys: Boolean = false
) : StatementOptions(name, applyNameToQuery, closeParameters, usePreparedStatement) {

    override val cacheKey = name

    fun copy(name: String? = this.name,
             applyNameToQuery: Boolean = this.applyNameToQuery,
             usePreparedStatement: Boolean = this.usePreparedStatement,
             useGeneratedKeys: Boolean = this.useGeneratedKeys,
             closeParameters: Boolean = this.closeParameters
    ): UpdateOptions = UpdateOptions(name, applyNameToQuery, usePreparedStatement, closeParameters, useGeneratedKeys)
}
