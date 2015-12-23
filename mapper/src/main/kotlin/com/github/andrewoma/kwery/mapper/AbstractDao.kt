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
import com.github.andrewoma.kwery.core.Cache
import com.github.andrewoma.kwery.core.ConcurrentHashMapCache
import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.StatementOptions
import com.github.andrewoma.kwery.mapper.listener.*
import java.sql.Array
import java.sql.SQLFeatureNotSupportedException
import java.util.*

abstract class AbstractDao<T : Any, ID : Any>(
        val session: Session,
        val table: Table<T, ID>,
        val id: (T) -> ID,
        val idSqlType: String? = null,
        override val defaultIdStrategy: IdStrategy = IdStrategy.Auto,
        val defaultId: ID? = null,
        val sqlCache: Cache<Any, String> = ConcurrentHashMapCache()
) : Dao<T, ID> {

    protected val nf: (Column<T, *>) -> String = { it.name }

    override val defaultColumns = table.defaultColumns

    protected val columns = table.defaultColumns.join()

    private val listeners = linkedSetOf<Listener>()

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    protected fun fireEvent(events: List<Event>) {
        listeners.forEach { it.onEvent(session, events) }
    }

    protected fun <T1, T2> List<Pair<T1, T2>>.join(apply: (T1, List<T2>) -> T1): List<T1> {
        return this.groupBy { it.first }.map { apply(it.key, it.value.map { it.second }) }
    }

    protected fun Iterable<Column<T, *>>.join(separator: String = ", ", f: (Column<T, *>) -> String = nf): String {
        return this.map { f(it) }.joinToString(separator)
    }

    protected fun Iterable<Column<T, *>>.equate(separator: String = ", ", f: (Column<T, *>) -> String = nf): String {
        return this.map { "${f(it) } = :${f(it)}" }.joinToString(separator)
    }

    protected fun Collection<ID>.copyToSqlArray(): java.sql.Array {
        return session.connection.createArrayOf(idSqlType, this.toTypedArray<Any>())
    }

    protected fun options(name: String): StatementOptions =
            session.defaultOptions.copy(name = this.javaClass.simpleName + "." + name)

    override fun findById(id: ID, columns: Set<Column<T, *>>): T? {
        val name = "findById"
        val sql = sql(name to columns) {
            "select ${columns.join()} \nfrom ${table.name} \nwhere ${table.idColumns.equate(" and ")}"
        }
        return session.select(sql, table.idMap(session, id, nf), options(name), table.rowMapper(columns)).firstOrNull()
    }

    override fun findAll(columns: Set<Column<T, *>>): List<T> {
        val name = "findAll"
        val sql = sql(name to columns) { "select ${columns.join()} \nfrom ${table.name}" }
        return session.select(sql, mapOf(), options(name), table.rowMapper(columns))
    }

    override fun findByExample(example: T, exampleColumns: Set<Column<T, *>>, columns: Set<Column<T, *>>): List<T> {
        val name = "findByExample"

        if (exampleColumns.isEmpty()) return findAll(columns)

        val exampleMap = table.objectMap(session, example, exampleColumns, nf)
        val sql = sql(Triple(name, exampleColumns, columns)) {
            "select ${columns.join()} \nfrom ${table.name}\nwhere ${exampleColumns.equate(" and ")}"
        }
        return session.select(sql, exampleMap, options(name), table.rowMapper(columns))
    }

    private fun isGeneratedKey(value: T?, strategy: IdStrategy): Boolean = when (strategy) {
        IdStrategy.Explicit -> false
        IdStrategy.Generated -> true
        IdStrategy.Auto -> {
            checkNotNull(value) { "Cannot calculate key strategy with null value" }
            id(value!!) == defaultId
        }
    }

    override fun update(oldValue: T, newValue: T, deltaOnly: Boolean): T {
        fireEvent(listOf(PreUpdateEvent(table, id(oldValue), newValue, oldValue)))
        val name = "update"
        require(id(oldValue) == id(newValue)) { "Attempt to update ${table.name} objects with different ids: ${id(oldValue)} ${id(newValue)}" }
        require(table is Versioned<*>) { "table must be Versioned to use update. Use unsafeUpdate for unversioned tables" }

        val versionColumn = table.versionColumn!!
        @Suppress("UNCHECKED_CAST")
        val newVersion = (table as Versioned<Any?>).nextVersion(versionColumn.property(oldValue))
        val result = table.copy(newValue, mapOf(versionColumn to newVersion))

        val oldMap = table.objectMap(session, oldValue, table.dataColumns)
        val newMap = table.objectMap(session, result, table.dataColumns)

        val versionCol = versionColumn.name
        val oldVersionParam = "old__$versionCol"

        fun delta(): Pair<String, Map<String, Any?>> {
            val differences = difference(oldMap, newMap)
            val sql = sql(name to differences) {
                val columns = differences.keys.map { "$it = :$it" }.joinToString(", ")
                "update ${table.name}\nset $columns \nwhere ${table.idColumns.equate(" and ")} and $versionCol = :$oldVersionParam"
            }
            val parameters = hashMapOfExpectedSize<String, Any?>(differences.size + table.idColumns.size + 1)
            parameters.putAll(differences)
            parameters.putAll(table.idMap(session, id(newValue), nf))
            parameters[oldVersionParam] = oldMap[versionCol]
            return sql to parameters
        }

        fun full(): Pair<String, HashMap<String, Any?>> {
            val sql = sql(name) {
                "update ${table.name}\nset ${table.dataColumns.equate()} \nwhere ${table.idColumns.equate(" and ")} and $versionCol = :$oldVersionParam"
            }
            val parameters = hashMapOfExpectedSize<String, Any?>(newMap.size + table.idColumns.size + 1)
            parameters.putAll(newMap)
            parameters.putAll(table.idMap(session, id(newValue), nf))
            parameters[oldVersionParam] = oldMap[versionCol]
            return sql to parameters
        }

        val (sql, parameters) = if (deltaOnly) delta() else full()

        val count = session.update(sql, parameters, options(name))
        if (count == 0) {
            throw OptimisticLockException("The same version (${oldMap[versionCol]}) of ${table.name} with id ${id(oldValue)} has been updated by another transaction")
        }

        fireEvent(listOf(UpdateEvent(table, id(oldValue), result, oldValue)))

        return result
    }

    private fun difference(lhs: Map<String, Any?>, rhs: Map<String, Any?>): Map<String, Any?> {
        val differences = linkedMapOf<String, Any?>()
        for ((key, value) in rhs) {
            if (value != lhs[key]) differences[key] = value
        }
        return differences
    }

    override fun delete(id: ID): Int {
        val name = "delete"
        val sql = sql(name) { "delete from ${table.name} where ${table.idColumns.equate(" and ")}" }
        val count = session.update(sql, table.idMap(session, id, nf), options(name))

        fireEvent(listOf(DeleteEvent(table, id, null)))

        return count
    }

    override fun unsafeUpdate(newValue: T): Int {
        fireEvent(listOf(PreUpdateEvent(table, id(newValue), newValue, null)))
        val name = "unsafeUpdate"

        val sql = sql(name) {
            "update ${table.name}\nset ${table.dataColumns.equate()} \nwhere ${table.idColumns.equate(" and ")}"
        }
        val newMap = table.objectMap(session, newValue, table.allColumns)
        return session.update(sql, newMap, options(name))
    }

    override fun batchInsert(values: List<T>, idStrategy: IdStrategy): List<T> {
        fireEvent(values.map { PreInsertEvent(table, id(it), it) })
        val name = "batchInsert"
        val generateKeys = isGeneratedKey(values.firstOrNull(), idStrategy)

        if (generateKeys && table.idColumns.size > 1) {
            throw UnsupportedOperationException("Batch insert with generated compound keys is unsupported")
        }

        val columns = if (generateKeys) table.dataColumns else table.allColumns
        val sql = sql(name) { "insert into ${table.name}(${columns.join()}) \nvalues (${columns.join { ":${it.name}" }})" }

        val inserted = if (generateKeys) {
            val list = session.batchInsert(sql, values.map { table.objectMap(session, it, columns, nf) }, options(name),
                    { table.rowMapper(table.idColumns, nf)(it) })

            val count = list.map { it.first }.fold(0) { sum, value -> sum + value }
            check(count == values.size) { "$name inserted $count rows, but expected ${values.size}" }

            values.zip(list.map { it.second }).map {
                val (value, idValue) = it
                table.copy(value, table.idColumns((id(idValue))).toMap())
            }
        } else {
            val counts = session.batchUpdate(sql, values.map { table.objectMap(session, it, columns, nf) }, options(name))
            val count = counts.fold(0) { sum, value -> sum + value }
            check(count == values.size) { "$name inserted $count rows, but expected ${values.size}" }
            values
        }

        fireEvent(inserted.map { InsertEvent(table, id(it), it) })

        return inserted
    }

    override fun insert(value: T, idStrategy: IdStrategy): T {
        fireEvent(listOf(PreInsertEvent(table, id(value), value)))
        val name = "insert"
        val generateKeys = isGeneratedKey(value, idStrategy)

        val columns = if (generateKeys) table.dataColumns else table.allColumns
        val sql = sql(name to columns) { "insert into ${table.name}(${columns.join()}) \nvalues (${columns.join { ":${it.name}" }})" }
        val parameters = table.objectMap(session, value, columns, nf)

        val (count, inserted) = if (generateKeys) {
            val (count, key) = session.insert(sql, parameters, options(name), { table.rowMapper(table.idColumns, nf)(it) })
            check(count == 1) { "$name failed to insert any rows" }
            count to table.copy(value, table.idColumns(id(key)).toMap()) // Generated key
        } else {
            val count = session.update(sql, parameters, options(name))
            count to value
        }
        check(count == 1) { "$name failed to insert any rows" }

        fireEvent(listOf(InsertEvent(table, id(inserted), inserted)))

        return inserted
    }

    override fun findByIds(ids: Collection<ID>, columns: Set<Column<T, *>>): Map<ID, T> {
        val name = "findByIds"
        if (ids.isEmpty()) return mapOf()

        if (ids.size == 1) {
            return findById(ids.first())?.let { mapOf(id(it) to it) } ?: mapOf()
        }

        // TODO ... support compound ids? No nice way of doing this without spamming statement caches
        if (table.idColumns.size != 1) throw UnsupportedOperationException("Find by ids with compound keys is currently unsupported")

        val values = if (session.dialect.supportsArrayBasedIn) {
            val sql = sql(name to columns) {
                "select ${columns.join()} \nfrom ${table.name} \nwhere ${table.idColumns.first().name} " +
                        session.dialect.arrayBasedIn("ids")
            }
            val array = ids.copyToSqlArray()
            try {
                session.select(sql, mapOf("ids" to array), options(name), table.rowMapper(columns))
            } finally {
                freeIfSupported(array)
            }
        } else {
            val sql = sql(name to columns) {
                "select ${columns.join()} \nfrom ${table.name} \nwhere ${table.idColumns.first().name} in (:ids)"
            }
            session.select(sql, mapOf("ids" to ids), options(name), table.rowMapper(columns))
        }

        return values.map { id(it) to it }.toMap()
    }

    private fun freeIfSupported(array: Array) {
        try {
            array.free()
        } catch(e: SQLFeatureNotSupportedException) {
            // Ignore and hope the driver cleans up properly
        }
    }

    protected fun sql(key: Any, f: () -> String): String = sqlCache.getOrPut(key, { f() })

    override fun unsafeBatchUpdate(values: List<T>) {
        for (value in values) {
            fireEvent(listOf(PreUpdateEvent(table, id(value), value, null)))
        }

        val name = "unsafeBatchUpdate"

        val updates = values.map { table.objectMap(session, it, table.allColumns) }

        val sql = sql(name) {
            "update ${table.name}\nset ${table.dataColumns.equate()} \nwhere ${table.idColumns.equate(" and ")}"
        }

        val counts = session.batchUpdate(sql, updates, options(name))

        check(counts.size == values.size) { "$name updated ${counts.size} rows, but expected ${values.size}" }

        for ((i, count) in counts.withIndex()) {
            check(count == 1) { "Batch update failed to update row with id ${id(values[i])}" }
        }

        for (value in values) {
            fireEvent(listOf(UpdateEvent(table, id(value), value, null)))
        }
    }

    protected fun version(value: T): Any {
        return table.objectMap(session, value, setOf(table.versionColumn!!)).values.first()!!
    }

    override fun batchUpdate(values: List<Pair<T, T>>): List<T> {
        for ((old, new) in values.asSequence().map { it.first }.zip(values.asSequence().map { it.second })) {
            fireEvent(listOf(PreUpdateEvent(table, id(old), new, old)))
        }

        val name = "batchUpdate"
        require(table is Versioned<*>) { "table must be Versioned to use batchUpdate. Use unsafeBatchUpdate for unversioned tables" }
        val versionColumn = table.versionColumn!!
        val versionCol = versionColumn.name
        val oldVersionParam = "old__$versionCol"

        val updates = values.map {
            val (old, new) = it
            require(id(old) == id(new)) { "Attempt to update ${table.name} objects with different ids: ${id(old)} ${id(new)}" }

            @Suppress("UNCHECKED_CAST")
            val newVersion = (table as Versioned<Any?>).nextVersion(versionColumn.property(old))

            val result = table.copy(new, mapOf(versionColumn to newVersion))
            val newMap = table.objectMap(session, result, table.dataColumns)

            val parameters = hashMapOfExpectedSize<String, Any?>(newMap.size + table.idColumns.size + 1)
            parameters.putAll(newMap)
            parameters.putAll(table.idMap(session, id(new), nf))
            parameters[oldVersionParam] = version(old)
            parameters to result
        }

        val sql = sql(name) {
            "update ${table.name}\nset ${table.dataColumns.equate()} \nwhere ${table.idColumns.equate(" and ")} and $versionCol = :$oldVersionParam"
        }

        val counts = session.batchUpdate(sql, updates.map { it.first }, options(name))
        check(counts.size == values.size) { "$name updated ${counts.size} rows, but expected ${values.size}" }
        for ((count, value) in counts.zip(values)) {
            if (count == 0) {
                throw OptimisticLockException("The same version (${version(value.first)}) of ${table.name} with id ${id(value.first)} has been updated by another transaction")
            }
        }

        for ((old, new) in values.asSequence().map { it.first }.zip(updates.asSequence().map { it.second })) {
            fireEvent(listOf(UpdateEvent(table, id(old), new, old)))
        }

        return updates.map { it.second }
    }

    override fun allocateIds(count: Int): List<ID> {
        require(session.dialect.supportsAllocateIds) { "Dialect does not support allocate ids" }
        require(table.sequence != null) { "Table sequence is not defined" }
        require(table.idColumns.size == 1) { "Compound ids are not supported" }

        val sql = session.dialect.allocateIds(count, table.sequence!!, table.idColumns.first().name)
        return session.select(sql, mapOf(), options("allocateIds")) { row ->
            id(table.rowMapper(table.idColumns, nf)(row))
        }
    }
}

enum class IdStrategy {
    /**
     * Auto will automatically set the strategy to Generated or Explicit based on whether a
     * non-default id value is provided in the value inserted
     */
    Auto,

    /**
     * Forces the use of generated keys
     */
    Generated,

    /**
     * Inserts the id from the value explicitly, not using generated keys
     */
    Explicit
}
