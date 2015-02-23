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

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.SelectOptions
import java.util.concurrent.ConcurrentHashMap
import com.github.andrewoma.kwery.core.UpdateOptions
import java.util.HashMap
import com.github.andrewoma.kommon.collection.hashMapOfExpectedSize

public enum class IdStrategy {
    /**
     * Auto will automatically set the strategy to Generated or Explicit based on whether a
     * non-default id value is provided in the value inserted
     */
    Auto

    /**
     * Forces the use of generated keys
     */
    Generated

    /**
     * Inserts the id from the value explicitly, not using generated keys
     */
    Explicit
}

public abstract class AbstractDao<T : Any, ID : Any>(
        val session: Session,
        val table: Table<T, ID>,
        val id: (T) -> ID,
        val idSqlType: String? = null,
        override val defaultIdStrategy: IdStrategy = IdStrategy.Auto,
        val defaultId: ID? = null
) : Dao<T, ID> {

    protected val nf: (Column<T, *>) -> String = { it.name }
    protected val sqlCache: MutableMap<Any, String> = ConcurrentHashMap()

    override val defaultColumns = table.defaultColumns

    val columns = table.defaultColumns.join()

    protected fun <T1, T2> List<Pair<T1, T2>>.join(apply: (T1, List<T2>) -> T1): List<T1> {
        return this.groupBy { it.first }.map { apply(it.key, it.value.map { it.second }) }
    }

    protected fun Iterable<Column<T, *>>.join(separator: String = ", ", f: (Column<T, *>) -> String = nf): String {
        return this.map { f(it) }.joinToString(separator)
    }

    protected fun Iterable<Column<T, *>>.equate(separator: String = ", ", f: (Column<T, *>) -> String = nf): String {
        return this.map { "${f(it) } = :${f(it)}" }.joinToString(separator)
    }

    protected fun selectOptions(name: String): SelectOptions =
            session.defaultSelectOptions.copy(name = this.javaClass.getSimpleName() + "." + name)

    protected fun updateOptions(name: String): UpdateOptions =
            session.defaultUpdateOptions.copy(name = this.javaClass.getSimpleName() + "." + name)

    override fun findById(id: ID, columns: Set<Column<T, *>>): T? {
        val name = "findById"
        val sql = sql(name to columns) {
            "select ${columns.join()} \nfrom ${table.name} \nwhere ${table.idColumns.equate(" and ")}"
        }
        return session.select(sql, table.idMap(session, id, nf), selectOptions(name), table.rowMapper(columns)).firstOrNull()
    }

    override fun findAll(columns: Set<Column<T, *>>): List<T> {
        val name = "findAll"
        val sql = sql(name to columns) { "select ${columns.join()} \nfrom ${table.name}" }
        return session.select(sql, mapOf(), selectOptions(name), table.rowMapper(columns))
    }

    override fun findByExample(example: T, exampleColumns: Set<Column<T, *>>, columns: Set<Column<T, *>>): List<T> {
        val name = "findByExample"
        val exampleMap = table.objectMap(session, example, exampleColumns, nf)
        val sql = sql(Triple(name, exampleColumns, columns)) {
            "select ${columns.join()} \nfrom ${table.name}\nwhere ${exampleColumns.equate(" and ")}"
        }
        return session.select(sql, exampleMap, selectOptions(name), table.rowMapper(columns))
    }

    private fun isGeneratedKey(value: T?, strategy: IdStrategy): Boolean = when (strategy) {
        IdStrategy.Explicit -> false
        IdStrategy.Generated -> true
        IdStrategy.Auto -> {
            checkNotNull(value, "Cannot calculate key strategy with null value")
            id(value!!) == defaultId
        }
    }

    override fun update(oldValue: T, newValue: T, deltaOnly: Boolean): T {
        val name = "update"
        check(id(oldValue) == id(newValue)) { "Attempt to update ${table.name} objects with different ids: ${id(oldValue)} ${id(newValue)}" }
        check(table is Versioned<*>) { "table must be Versioned to use update. Use unsafeUpdate for unversioned tables" }

        val versionColumn = table.versionColumn!!
        [suppress("UNCHECKED_CAST")]
        val newVersion = (table as Versioned<Any?>).nextVersion(versionColumn.property(oldValue))
        val result = table.copy(newValue, mapOf(versionColumn to newVersion))

        val oldMap = table.objectMap(session, oldValue, table.dataColumns)
        val newMap = table.objectMap(session, result, table.dataColumns)

        val versionCol = versionColumn.name
        val oldVersionParam = "old__${versionCol}"

        fun delta(): Pair<String, Map<String, Any?>> {
            val differences = difference(oldMap, newMap)
            val sql = sql(name to differences) {
                val columns = differences.keySet().map { "${it} = :${it}" }.joinToString(", ")
                "update ${table.name}\nset $columns \nwhere ${table.idColumns.equate(" and ")} and ${versionCol} = :$oldVersionParam"
            }
            val parameters = hashMapOfExpectedSize<String, Any?>(differences.size() + table.idColumns.size() + 1)
            parameters.putAll(differences)
            parameters.putAll(table.idMap(session, id(newValue), nf))
            parameters[oldVersionParam] = oldMap[versionCol]
            return sql to parameters
        }

        fun full(): Pair<String, HashMap<String, Any?>> {
            val sql = sql(name) {
                "update ${table.name}\nset ${table.dataColumns.equate()} \nwhere ${table.idColumns.equate(" and ")} and ${versionCol} = :$oldVersionParam"
            }
            val parameters = hashMapOfExpectedSize<String, Any?>(newMap.size() + table.idColumns.size() + 1)
            parameters.putAll(newMap)
            parameters.putAll(table.idMap(session, id(newValue), nf))
            parameters[oldVersionParam] = oldMap[versionCol]
            return sql to parameters
        }

        val (sql, parameters) = if (deltaOnly) delta() else full()

        val count = session.update(sql, parameters, updateOptions(name))
        if (count == 0) {
            throw OptimisticLockException("The same version (${oldMap[versionCol]}) of ${table.name} with id ${id(oldValue)} has been updated by another transaction")
        }

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
        val count = session.update(sql, table.idMap(session, id, nf), updateOptions(name))

        return count
    }

    override fun unsafeUpdate(newValue: T) {
        throw UnsupportedOperationException("TODO") // TODO
    }

    override fun batchInsert(values: List<T>, idStrategy: IdStrategy): List<T> {
        val generateKeys = isGeneratedKey(values.firstOrNull(), idStrategy)

        if (generateKeys && table.idColumns.size() > 1) {
            throw UnsupportedOperationException("Batch insert with generated compound keys is unsupported")
        }

        val name = "batchInsert"
        val columns = if (generateKeys) table.dataColumns else table.allColumns
        val sql = sql(name) { "insert into ${table.name}(${columns.join()}) \nvalues (${columns.join { ":${it.name}" }})" }

        if (generateKeys) {
            val list = session.batchUpdate(sql, values.map { table.objectMap(session, it, columns, nf) }, updateOptions(name),
                    { table.rowMapper(table.idColumns, nf)(it) })

            val count = list.map { it.first }.fold(0) {(sum, value) -> sum + value }
            check(count == values.size(), "${name} inserted $count rows, but expected ${values.size()}")

            return values.zip(list.map { it.second }).map {
                val (value, idValue) = it
                table.copy(value, table.idColumns((id(idValue))).toMap())
            }
        } else {
            val counts = session.batchUpdate(sql, values.map { table.objectMap(session, it, columns, nf) }, updateOptions(name))
            val count = counts.fold(0) {(sum, value) -> sum + value }
            check(count == values.size(), "${name} inserted $count rows, but expected ${values.size()}")
            return values
        }
    }

    override fun insert(value: T, idStrategy: IdStrategy): T {
        val generateKeys = isGeneratedKey(value, idStrategy)

        val name = "insert"
        val columns = if (generateKeys) table.dataColumns else table.allColumns
        val sql = sql(name to columns) { "insert into ${table.name}(${columns.join()}) \nvalues (${columns.join { ":${it.name}" }})" }
        val parameters = table.objectMap(session, value, columns, nf)

        val (count, inserted) = if (generateKeys) {
            val (count, key) = session.update(sql, parameters, updateOptions(name), { table.rowMapper(table.idColumns, nf)(it) })
            check(count == 1, "${name} failed to insert any rows")
            count to table.copy(value, table.idColumns(id(key)).toMap()) // Generated key
        } else {
            val count = session.update(sql, parameters, updateOptions(name))
            count to value
        }
        check(count == 1, "${name} failed to insert any rows")
        return inserted
    }

    override fun findByIds(ids: Collection<ID>, columns: Set<Column<T, *>>): Map<ID, T> {
        if (ids.isEmpty()) return mapOf()

        val name = "findByIds"

        if (table.idColumns.size() != 1) throw UnsupportedOperationException("Find by ids with compound keys is currently unsupported")

        val (sql, idsParam) = if (session.dialect.supportsArrayBasedIn) {
            val sql = sql(name to columns) {
                "select ${columns.join()} \nfrom ${table.name} \nwhere ${table.idColumns.first().name} " +
                        session.dialect.arrayBasedIn("ids")
            }

            sql to session.connection.createArrayOf(idSqlType, ids.copyToArray<Any>())
        } else {
            val sql = sql(name to columns) {
                "select ${columns.join()} \nfrom ${table.name} \nwhere ${table.idColumns.first().name} in (:ids)"
            }
            sql to ids.map { table.idMap(session, it, nf).values().first() }
        }

        val values = session.select(sql, mapOf("ids" to idsParam), selectOptions(name), table.rowMapper(columns))
        return values.map { id(it) to it }.toMap()
    }

    protected inline fun sql(key: Any, f: () -> String): String = sqlCache.getOrPut(key, f)

    override fun batchUpdate(values: List<T>): List<T> {
        // TODO
        throw UnsupportedOperationException()
    }
}