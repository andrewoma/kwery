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

package com.github.andrewoma.kwery.mapper

import kotlin.reflect.KMemberProperty
import java.util.LinkedHashSet
import kotlin.properties.Delegates
import com.github.andrewoma.kwery.core.Row
import com.github.andrewoma.kommon.collection.hashMapOfExpectedSize
import kotlin.properties.ReadOnlyProperty
import com.github.andrewoma.kwery.core.Session

public data class Column<T, R>(val property: (T) -> R,
                               val defaultValue: R = null,
                               val converter: Converter<R>,
                               val name: String,
                               val id: Boolean,
                               val version: Boolean,
                               val selectByDefault: Boolean,
                               val isNullable: Boolean
) {
    public fun of(value: R): Pair<Column<T, R>, R> = Pair(this, value)

    override fun toString(): String {
        return "Column($name id=$id version=$version nullable=$isNullable)" // Prevent NPE in debugger on "property"
    }
}

public trait Value<T> {
    fun <R> of(column: Column<T, R>): R
}

class TableConfiguration(val defaults: Map<Class<*>, *>, val converters: Map<Class<*>, Converter<*>>, val namingConvention: (String) -> String)

public abstract class Table<T : Any, ID>(val name: String, val config: TableConfiguration, val sequence: String? = null) {
    public val allColumns: Set<Column<T, *>> = LinkedHashSet()
    public val defaultColumns: Set<Column<T, *>> by Delegates.lazy { initialise(); LinkedHashSet(allColumns.filter { it.selectByDefault }) }
    public val idColumns: Set<Column<T, *>> by Delegates.lazy { initialise(); LinkedHashSet(allColumns.filter { it.id }) }
    public val dataColumns: Set<Column<T, *>> by Delegates.lazy { initialise(); LinkedHashSet(allColumns.filterNot { it.id }) }
    public val versionColumn: Column<T, *>? by Delegates.lazy { initialise(); allColumns.firstOrNull { it.version } }
    public val type: Class<T> by Delegates.lazy { initialise(); lazyType!! }

    private val columnName: (Column<T, *>) -> String = { it.name }
    private var initialised = false
    private var lazyType: Class<T>? = null

    public abstract fun create(value: Value<T>): T
    public abstract fun idColumns(id: ID): Set<Pair<Column<T, *>, *>>

    public fun <R> addColumn(column: Column<T, R>): Column<T, R> {
        [suppress("UNCHECKED_CAST")]
        (allColumns as MutableSet<Any?>).add(column)
        return column
    }

    // Indirectly calls "get" on all delegated columns, which then adds them to the "allColumns"
    private fun initialise() {
        synchronized(this) {
            if (initialised) return

            val instance: T
            try {
                instance = create(object : Value<T> {
                                override fun <R> of(column: Column<T, R>): R {
                                    return column.defaultValue
                                }
                            })
            } catch(e: NullPointerException) {
                throw RuntimeException("A table field is declared as nullable but the mapped field is non-null?", e)
            }
            lazyType = instance.javaClass
            initialised = true
        }
    }

    // A delegated property that gets the column name from the property name unless it is defined
    public inner class DelegatedColumn<R>(val template: Column<T, R>, private var value: Column<T, R>? = null) : ReadOnlyProperty<Any?, Column<T, R>> {
        public override fun get(thisRef: Any?, desc: PropertyMetadata): Column<T, R> {
            if (value == null) {
                value = addColumn(template.copy(name = template.name.let { if (it != "") it else config.namingConvention(desc.name) }))
            }
            return value!!
        }
    }

    public inline fun <reified R> col(property: KMemberProperty<T, R>,
                                      id: Boolean = false,
                                      version: Boolean = false,
                                      notNull: Boolean = id || version,
                                      default: R = default(!notNull, javaClass<R>()),
                                      converter: Converter<R> = converter(!notNull, javaClass<R>()),
                                      name: String? = null,
                                      selectByDefault: Boolean = true): DelegatedColumn<R> {

        val column = Column<T, R>({ property.get(it) }, default, converter, name ?: "", id, version, selectByDefault, !notNull)
        return DelegatedColumn(column)
    }

    public inline fun <C, reified R> col(property: KMemberProperty<C, R>,
                                         noinline path: (T) -> C,
                                         id: Boolean = false,
                                         version: Boolean = false,
                                         notNull: Boolean = id || version,
                                         default: R = default(!notNull, javaClass<R>()),
                                         converter: Converter<R> = converter(!notNull, javaClass<R>()),
                                         name: String? = null,
                                         selectByDefault: Boolean = true

    ): DelegatedColumn<R> {
        val column = Column<T, R>({ property.get(path(it)) }, default, converter, name ?: "", id, version, selectByDefault, !notNull)
        return DelegatedColumn(column)
    }

    // Can't cast T to Enum<T> due to recursive type, so cast to any enum to satisfy compiler
    private enum class DummyEnum

    suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_UNIT_OR_ANY", "CAST_NEVER_SUCCEEDS")
    public fun <T> converter(nullable: Boolean, type: Class<T>): Converter<T> {
        val converter = config.converters[type] ?: if (type.isEnum()) EnumByNameConverter(type as Class<DummyEnum>) as T else null
        checkNotNull(converter, "Converter undefined for type: ${type.getName()}")
        return (if (nullable) optional(converter!! as Converter<T>) else converter) as Converter<T>
    }

    public fun <T> default(nullable: Boolean, type: Class<T>): T {
        if (nullable) return null
        val value = config.defaults[type]
        checkNotNull(value, "Default value undefined for type: ${type.getName()}")
        [suppress("UNCHECKED_CAST")]
        return value as T
    }

    public fun copy(value: T, properties: Map<Column<T, *>, *>): T {
        return create(object : Value<T> {
            override fun <R> of(column: Column<T, R>): R {
                [suppress("UNCHECKED_CAST")]
                return if (properties.contains(column)) properties[column] as R else column.property(value)
            }
        })
    }

    public fun objectMap(session: Session, value: T, columns: Set<Column<T, *>> = defaultColumns, nf: (Column<T, *>) -> String = columnName): Map<String, Any?> {
        val map = hashMapOfExpectedSize<String, Any?>(columns.size())
        for (column in columns) {
            [suppress("UNCHECKED_CAST")] // Compiler crashes without cast
            (map[nf(column)] = (column as Column<T, Any?>).converter.to(session.connection, column.property(value)))
        }
        return map
    }

    public fun idMap(session: Session, id: ID, nf: (Column<T, *>) -> String = columnName): Map<String, Any?> {
        val idCols = idColumns(id)
        val map = hashMapOfExpectedSize<String, Any?>(idCols.size())
        for ((column, value) in idCols) {
            [suppress("UNCHECKED_CAST")] // Compiler crashes without cast
            (map[nf(column)] = (column as Column<T, Any?>).converter.to(session.connection, value))
        }
        return map
    }

    public fun rowMapper(columns: Set<Column<T, *>> = defaultColumns, nf: (Column<T, *>) -> String = columnName): (Row) -> T {
        return { row ->
            create(object : Value<T> {
                override fun <R> of(column: Column<T, R>): R {
                    return if (columns.contains(column)) column.converter.from(row, nf(column)) else column.defaultValue
                }
            })
        }
    }
}