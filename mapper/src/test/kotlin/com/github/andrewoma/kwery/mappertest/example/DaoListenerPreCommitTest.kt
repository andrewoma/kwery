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

package com.github.andrewoma.kwery.mappertest.example

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.listener.*
import com.github.andrewoma.kwery.mappertest.AbstractSessionTest
import com.github.andrewoma.kwery.mappertest.example.test.initialiseFilmSchema
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import org.junit.Test

/**
 * Demonstrates a pre commit listener the saves audit records of all changes
 */
class DaoListenerPreCommitTest : AbstractSessionTest() {
    var dao: ActorDao by Delegates.notNull()
    var nextTransactionId = 0

    override var startTransactionByDefault = false

    override fun afterSessionSetup() {
        initialise("filmSchema") { initialiseFilmSchema(it) }
        initialise("audit") {
            it.update("""
                create table audit(
                    id          identity,
                    transaction numeric(10),
                    table_name  varchar(255),
                    key         numeric(10),
                    operation   varchar(255),
                    changes     varchar(4000)
                )
            """)
        }
        super.afterSessionSetup()

        dao = ActorDao(session, FilmActorDao(session))
        dao.addListener(AuditHandler())
        session.update("delete from actor where actor_id > -1000")
        session.update("delete from audit")
    }

    @Test fun `Audit rows should be created for each transaction`() {
        val transactionId = nextTransactionId + 1

        val (bruce, brandon) = session.transaction {
            val bruce = dao.insert(Actor(Name("Bruce", "Lee")))
            val brandon = dao.insert(Actor(Name("Brandon", "Lee")))
            dao.update(brandon, brandon.copy(name = (Name("Tex", "Lee"))))
            bruce to brandon
        }

        // Generates:
        //   Audit(transactionId=1, table=actor, id=0, operation=Insert, changes=last_name: 'Lee', first_name: 'Bruce', last_update: '2015-02-26 05:44:49.796')
        //   Audit(transactionId=1, table=actor, id=1, operation=Insert, changes=last_name: 'Lee', first_name: 'Brandon', last_update: '2015-02-26 05:44:49.796')
        //   Audit(transactionId=1, table=actor, id=1, operation=Update, changes=first_name: 'Brandon' -> 'Tex', last_update: '2015-02-26 05:44:49.796' -> '2015-02-26 05:44:49.938')

        session.transaction {
            dao.delete(bruce.id)
        }

        // Generates:
        //   Audit(transactionId=2, table=actor, id=0, operation=Delete, changes=)

        // This should have no effect as audits will be rolled back in the transaction too
        session.transaction {
            session.currentTransaction?.rollbackOnly = true
            dao.delete(brandon.id)
        }

        val audits = session.select("select transaction, count(*) as \"count\" from audit group by transaction") { row ->
            row.int("transaction") to row.int("count")
        }.toMap()

        assertEquals(2, audits.size())
        assertEquals(3, audits.get(transactionId))
        assertEquals(1, audits.get(transactionId + 1))
    }

    data class Audit(val transactionId: Int, val table: String, val id: Int, val operation: String, val changes: String)

    inner class AuditHandler : DeferredListener(false) {
        override fun onCommit(committed: Boolean, events: List<Event>) {
            println("AuditHandler invoked")

            val transactionId = ++nextTransactionId
            val audits = events.map { event ->
                @suppress("UNCHECKED_CAST") // TODO ... grok projections, why is casting needed?
                val table = event.table as Table<Any, Any>
                val operation = event.javaClass.getSimpleName().replace("Event$".toRegex(), "")
                Audit(transactionId, event.table.name, event.id as Int, operation, calculateChanges(event, session, table))
            }

            println(audits.joinToString("\n"))
            saveAudits(audits, session)
        }

        private fun calculateChanges(event: Event, session: Session, table: Table<Any, Any>) = when (event) {
            is InsertEvent -> {
                table.objectMap(session, event.value, table.dataColumns).entrySet().map {
                    "${it.key}: ${session.dialect.bind(it.value!!, -1)}"
                }.joinToString(", ")
            }
            is UpdateEvent -> {
                val old = table.objectMap(session, event.old!!, table.dataColumns)
                val new = table.objectMap(session, event.new!!, table.dataColumns)
                old.mapValues { it.value to new[it.key] }.filter { it.value.first != it.value.second }.map {
                    "${it.key}: ${session.dialect.bind(it.value.first!!, -1)} -> ${session.dialect.bind(it.value.second!!, -1)}"
                }.joinToString(", ")
            }
            is DeleteEvent -> ""
            else -> throw UnsupportedOperationException("Unknown event: $event")
        }

        private fun saveAudits(audits: List<Audit>, session: Session) {
            val insert = """
                insert into audit(transaction, table_name, key, operation, changes)
                values (:transaction, :table_name, :key, :operation, :changes)
            """
            val params = audits.map {
                mapOf("transaction" to it.transactionId,
                        "table_name" to it.table,
                        "key" to it.id,
                        "operation" to it.operation,
                        "changes" to it.changes)
            }
            session.batchUpdate(insert, params)
        }
    }
}
