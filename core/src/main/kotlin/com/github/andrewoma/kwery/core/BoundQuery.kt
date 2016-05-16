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

import java.util.regex.Pattern

data class BoundQuery(val originalQuery: String, val query: String, val bindings: List<String>)

internal fun BoundQuery(query: String, inClauseSizes: Map<String, Int>): BoundQuery {
    val bindings = arrayListOf<String>()
    val bound = replaceBindings(query) { key ->
        bindings.add(key)
        val size = inClauseSizes[key] ?: 1
        Array(size) { "?" }.joinToString(",")
    }
    return BoundQuery(query, bound, bindings)
}

internal inline fun replaceBindings(query: String, onBinding: (String) -> String): String {
    val pattern = Pattern.compile("""\:([a-zA-Z_]+)""")
    val matcher = pattern.matcher(query)

    val result = StringBuffer()
    while (matcher.find()) {
        val key = matcher.group(1)
        matcher.appendReplacement(result, onBinding(key))
    }
    matcher.appendTail(result)
    return result.toString()
}

internal fun inClauseSizes(parametersList: List<Map<String, Any?>>): Map<String, Int> {
    val sizes: MutableMap<String, Int> = hashMapOf()

    for (parameters in parametersList) {
        for ((key, value) in parameters) {
            if (value is Collection<*> && value.size != 0) {
                sizes[key] = Math.max(inClauseSize(value.size), sizes[key] ?: 0)
            }
        }
    }

    return sizes
}

private fun inClauseSize(size: Int): Int {
    var num = size
    var count = 1
    while (num != 0) {
        num = num shr 1
        count++
    }
    if (count < 3) count = 3
    return Math.pow(2.toDouble(), count.toDouble()).toInt()
}
