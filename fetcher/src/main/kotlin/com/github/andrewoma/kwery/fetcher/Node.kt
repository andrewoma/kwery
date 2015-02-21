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

package com.github.andrewoma.kwery.fetcher

import java.util.Stack
import java.util.LinkedHashSet
import java.util.StringTokenizer
import kotlin.reflect.KMemberProperty

public data class Node(val name: String = "", val children: Set<Node> = setOf()) {
    class object {
        val empty = Node()
        val all = Node("*")
        val allDescendants = Node("**")

        fun parse(graph: String): Node {
            val nodes = Stack<Node>()
            val root = Node("", LinkedHashSet())
            nodes.push(root)

            val tokenizer = StringTokenizer(graph, ",()", true)
            while (tokenizer.hasMoreTokens()) {
                val token = tokenizer.nextToken().trim()
                if (token == ",") continue
                if (token == "(") {
                    nodes.push(nodes.peek().children.last())
                } else if (token == ")") {
                    nodes.pop()
                } else {
                    val node = when (token) {
                        "*" -> Node.all
                        "**" -> Node.allDescendants
                        else -> Node(token, LinkedHashSet())
                    }
                    (nodes.peek().children as MutableSet<Node>).add(node)
                }
            }

            root.initialise()
            return root
        }
    }

    private fun initialise() {
        childrenByName = buildChildrenByName()
        for (child in children) child.initialise()
    }

    private var childrenByName = buildChildrenByName()

    private fun buildChildrenByName() = children.map { it.name to it }.toMap()

    fun get(name: String) = childrenByName[name]

    override fun toString(): String {
        val isRoot = name == ""
        val cs = if (children.isEmpty()) "" else {
            children.joinToString(", ", (if (isRoot) "" else "("), (if (isRoot) "" else ")"))
        }
        return "$name$cs"
    }
}

public fun Node(vararg children: Node): Node = Node("", children.toSet())

public fun KMemberProperty<*, *>.node(vararg children: Node): Node = Node(this.name, children.toSet())
