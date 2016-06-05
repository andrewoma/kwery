package com.github.andrewoma.kwery.core.builder

import java.util.*

/*
 * Copyright (c) 2016 Andrew O'Malley
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

/**
 * QueryBuilder is a simple SQL query builder to ease building dynamic queries
 * where clauses and parameters may be optional.
 *
 * It is not a type-safe builder, it simply allows fragments of an SQL statement
 * to be added in any order and then builds the statement.
 *
 * The main feature is provides is allowing where clauses to be combined with
 * an operator, taking care of cases where groups end up being empty.
 */
class QueryBuilder {
    private val selects = LinkedHashSet<String>()
    private val params = mutableMapOf<String, Any?>()
    private var filters: Filter.Group = Filter.Group()
    private var groupBy: String? = null
    private var having: String? = null
    private var orderBy: String? = null

    fun select(table: String) {
        selects.add(table)
    }

    fun parameter(name: String, value: Any?) {
        params.put(name, value)
    }

    fun groupBy(groupBy: String) {
        this.groupBy = groupBy
    }

    fun having(having: String) {
        this.having = having
    }

    fun orderBy(orderBy: String) {
        this.orderBy = orderBy
    }

    fun whereGroup(operator: String = "and", block: FilterBuilder.() -> Unit) {
        require(filters.countLeaves() == 0) { "There must be only one root filters group" }
        filters = Filter.Group(operator)
        block(FilterBuilder(filters))
    }

    inner class FilterBuilder(private val group: Filter.Group) {

        fun where(where: String) {
            group.filters.add(Filter.Where(where))
        }

        fun whereGroup(operator: String = "and", block: FilterBuilder.() -> Unit) {
            val inner = Filter.Group(operator)
            group.filters.add(inner)
            block(FilterBuilder(inner))
        }
    }

    fun build(sb: StringBuilder = StringBuilder(), block: QueryBuilder.() -> Unit): Query {
        block(this)

        selects.joinTo(sb, "\n")
        if (filters.countLeaves() != 0) {
            sb.append("\nwhere ")
            appendConditions(sb, filters, true)
        }
        groupBy?.run { sb.append("\ngroup by ").append(groupBy) }
        having?.run { sb.append("\nhaving ").append(having) }
        orderBy?.run { sb.append("\norder by ").append(orderBy) }

        return Query(sb, params)
    }

    private fun appendConditions(sb: StringBuilder, conditions: Filter, root: Boolean) {
        when (conditions) {
            is Filter.Where -> sb.append(conditions.where)
            is Filter.Group -> appendConditions(conditions, root, sb)
        }
    }

    private fun appendConditions(conditions: Filter.Group, root: Boolean, sb: StringBuilder) {
        val filtered = conditions.filters.filter { it.countLeaves() != 0 }
        when (filtered.size) {
            0 -> Unit
            1 -> appendConditions(sb, filtered.first(), false)
            else -> {
                if (!root) sb.append("\n(")
                for ((i, condition) in filtered.withIndex()) {
                    appendConditions(sb, condition, false)
                    if (i != filtered.indices.last) sb.append(" ${conditions.operator} ")
                }
                if (!root) sb.append(")")
            }
        }
    }
}

fun query(sb: StringBuilder = StringBuilder(), block: QueryBuilder.() -> Unit): Query {
    return QueryBuilder().build(sb, block)
}
