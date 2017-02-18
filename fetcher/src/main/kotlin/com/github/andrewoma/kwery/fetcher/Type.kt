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

import kotlin.reflect.KProperty1

inline fun <ID, reified T : Any> Type(noinline id: (T) -> ID, noinline fetch: (Collection<ID>) -> Map<ID, T>, properties: List<BaseProperty<*, *, *>> = listOf()): Type<T, ID> {
    return Type(T::class.java, id, fetch, properties)
}

inline fun <ID, reified T : Any> Type(id: KProperty1<T, ID>, noinline fetch: (Collection<ID>) -> Map<ID, T>, properties: List<BaseProperty<*, *, *>> = listOf()): Type<T, ID> {
    return Type(T::class.java, { id.get(it) }, fetch, properties)
}

open class Type<T, ID>(
        val javaClass: Class<T>,
        val id: (T) -> ID,
        val fetch: (Collection<ID>) -> Map<ID, T>,
        var properties: List<BaseProperty<*, *, *>> = listOf() // Mutable to support cycles
) {
    open fun supports(obj: Any?) = obj?.let { it::class.java }?.isAssignableFrom(javaClass)!!

    override fun toString() = javaClass.simpleName + "(" + properties.map { it.name }.joinToString(", ") + ")"
}

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
open class BaseProperty<C, T, ID>(
        val id: (C) -> ID?,
        val type: Type<T, ID>,
        val name: String
)

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
class Property<C, T, ID>(
        val get: (C) -> T?,
        type: Type<T, ID>,
        id: (C) -> ID?,
        val apply: (C, T) -> C,
        name: String
) : BaseProperty<C, T, ID>(id, type, name)

@Suppress("BASE_WITH_NULLABLE_UPPER_BOUND")
fun <C, T, ID> Property(property: KProperty1<C, T?>,
                        type: Type<T, ID>,
                        id: (C) -> ID?,
                        apply: (C, T) -> C
): Property<C, T, ID> = Property({ property.get(it) }, type, id, apply, property.name)

class CollectionProperty<C, T, ID>(
        type: Type<T, ID>,
        id: (C) -> ID,
        val apply: (C, Collection<T>) -> C,
        val fetch: (Collection<ID>) -> Map<ID, Collection<T>>,
        name: String
) : BaseProperty<C, T, ID>(id, type, name) {
    override fun toString() = name
}

fun <C, T, ID> CollectionProperty(
        property: KProperty1<C, Collection<T>>,
        type: Type<T, ID>,
        id: (C) -> ID,
        apply: (C, Collection<T>) -> C,
        fetch: (Collection<ID>) -> Map<ID, Collection<T>>
): CollectionProperty<C, T, ID> = CollectionProperty(type, id, apply, fetch, property.name)
