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

import com.github.andrewoma.kommon.collection.hashMapOfExpectedSize
import com.github.andrewoma.kwery.core.Row
import com.github.andrewoma.kwery.core.Session
import java.lang.reflect.ParameterizedType
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.*
import kotlin.reflect.jvm.javaType


/**
 * A `Table` maps directly to a single SQL table, with each SQL column defined explicitly.
 */
abstract class Table<T : Any, ID>(val name: String, val config: TableConfiguration = TableConfiguration(), val sequence: String? = null) {

    val allColumns: Set<Column<T, *>> = LinkedHashSet()
    val defaultColumns: Set<Column<T, *>> by lazy { LinkedHashSet(allColumns.filter { it.selectByDefault }) }
    val idColumns: Set<Column<T, *>> by lazy { LinkedHashSet(allColumns.filter { it.id }) }
    val dataColumns: Set<Column<T, *>> by lazy { LinkedHashSet(allColumns.filterNot { it.id }) }
    val versionColumn: Column<T, *>? by lazy { allColumns.firstOrNull { it.version } }
    val type: Class<T> by lazy { lazyType!! }

    private val columnName: (Column<T, *>) -> String = { it.name }
    private var initialised = false
    private var lazyType: Class<T>? = null

    abstract fun create(value: Value<T>): T
    abstract fun idColumns(id: ID): Set<Pair<Column<T, *>, *>>

    fun <R> addColumn(column: Column<T, R>): Column<T, R> {
        @Suppress("UNCHECKED_CAST")
        (allColumns as MutableSet<Any?>).add(column)
        return column
    }

    private fun <T> lazy(f: () -> T) = kotlin.lazy(LazyThreadSafetyMode.NONE) { initialise(); f() }

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
    inner class DelegatedColumn<R>(val template: Column<T, R>, private var value: Column<T, R>? = null) : ReadOnlyProperty<Any?, Column<T, R>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Column<T, R> {
            if (value == null) {
                value = addColumn(template.copy(name = template.name.let { if (it != "") it else config.namingConvention(property.name) }))
            }
            return value!!
        }
    }

    fun <R> col(property: KProperty1<T, R>,
                id: Boolean = false,
                version: Boolean = false,
                notNull: Boolean = !property.returnType.isMarkedNullable,
                default: R = default(property.returnType),
                converter: Converter<R> = converter(property.returnType),
                name: String? = null,
                selectByDefault: Boolean = true): DelegatedColumn<R> {

        val column = Column<T, R>({ property.get(it) }, default, converter, name ?: "", id, version, selectByDefault, !notNull)
        return DelegatedColumn(column)
    }

    fun <C, R> col(property: KProperty1<C, R>,
                   path: (T) -> C,
                   id: Boolean = false,
                   version: Boolean = false,
                   notNull: Boolean = !property.returnType.isMarkedNullable,
                   default: R = default<R>(property.returnType),
                   converter: Converter<R> = converter(property.returnType),
                   name: String? = null,
                   selectByDefault: Boolean = true

    ): DelegatedColumn<R> {

        val column = Column<T, R>({ property.get(path(it)) }, default, converter, name ?: "", id, version, selectByDefault, !notNull)
        return DelegatedColumn(column)
    }

    /**
     * A column variant for cases where the property value is not null but the column
     * is optional via the path
     */
    fun <C, R> optionalCol(property: KProperty1<C, R>,
                           path: (T) -> C?,
                           id: Boolean = false,
                           version: Boolean = false,
                           converter: Converter<R> = converter(property.returnType),
                           name: String? = null,
                           selectByDefault: Boolean = true

    ): DelegatedColumn<R?> {

        val column = Column<T, R?>({ path(it)?.let { property.get(it) } }, null, optional(converter), name ?: "", id, version, selectByDefault, true)
        return DelegatedColumn(column)
    }


    // Can't cast T to Enum<T> due to recursive type, so cast to any enum to satisfy compiler
    private enum class DummyEnum

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_UNIT_OR_ANY", "CAST_NEVER_SUCCEEDS")
    protected fun <T> converter(type: KType): Converter<T> {
        // TODO ... converters are currently defined as Java classes as I can't figure out how to
        // convert a nullable KType into its non-nullable equivalent
        // Try udalov's workaround: (t.javaType as Class<*>).kotlin.defaultType`
        val javaClass = type.javaType as Class<T>
        val converter = config.converters[javaClass] ?: if (javaClass.isEnum) EnumByNameConverter(javaClass as Class<DummyEnum>) as T else null
        checkNotNull(converter) { "Converter undefined for type $type as $javaClass" }
        return (if (type.isMarkedNullable) optional(converter!! as Converter<Any>) else converter) as Converter<T>
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T> default(type: KType): T {
        if (type.isMarkedNullable) return null as T
        val value = config.defaults[type]

        if (value == null && type.isErasedType(List::class)) {
            return emptyList<Any?>() as T
        }

        checkNotNull(value) { "Default value undefined for type $type" }
        return value as T
    }

    private fun KType.isErasedType(clazz: KClass<*>)
            = this.javaType is ParameterizedType && (this.javaType as ParameterizedType).rawType == clazz.defaultType.javaType

    fun copy(value: T, properties: Map<Column<T, *>, *>): T {
        return create(object : Value<T> {
            override fun <R> of(column: Column<T, R>): R {
                @Suppress("UNCHECKED_CAST")
                return if (properties.contains(column)) properties[column] as R else column.property(value)
            }
        })
    }

    fun objectMap(session: Session, value: T, columns: Set<Column<T, *>> = defaultColumns, nf: (Column<T, *>) -> String = columnName): Map<String, Any?> {
        val map = hashMapOfExpectedSize<String, Any?>(columns.size)
        for (column in columns) {
            @Suppress("UNCHECKED_CAST")
            val col = column as Column<T, Any?>
            map[nf(column)] = col.converter.to(session.connection, column.property(value))
        }
        return map
    }

    fun idMap(session: Session, id: ID, nf: (Column<T, *>) -> String = columnName): Map<String, Any?> {
        val idCols = idColumns(id)
        val map = hashMapOfExpectedSize<String, Any?>(idCols.size)
        for ((column, value) in idCols) {
            @Suppress("UNCHECKED_CAST")
            val col = column as Column<T, Any?>
            map[nf(column)] = col.converter.to(session.connection, value)
        }
        return map
    }

    fun rowMapper(columns: Set<Column<T, *>> = defaultColumns, nf: (Column<T, *>) -> String = columnName): (Row) -> T {
        return { row ->
            create(object : Value<T> {
                override fun <R> of(column: Column<T, R>): R {
                    return if (columns.contains(column)) column.converter.from(row, nf(column)) else column.defaultValue
                }
            })
        }
    }
}