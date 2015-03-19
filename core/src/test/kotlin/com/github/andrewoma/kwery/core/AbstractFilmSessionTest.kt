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

import org.junit.Assert.assertEquals
import java.sql.Timestamp

data class Actor(val firstName: String, val lastName: String?, val id: Int = 0, val lastUpdate: Timestamp = Timestamp(System.currentTimeMillis()))

open class AbstractFilmSessionTest : AbstractSessionTest() {

    override fun afterSessionSetup() = initialise(javaClass<AbstractFilmSessionTest>().getName()) {
        //language=SQL
        val sql = """
            create table actor (
                actor_id    integer identity,
                first_name  character varying(45) not null,
                last_name   character varying(45) null,
                last_update timestamp             not null
            )
        """

        for (statement in sql.split(";")) {
            session.update(statement)
        }
    }

    fun Actor.toMap(): Map<String, Any?> = mapOf(
            "first_name" to this.firstName,
            "last_name" to this.lastName,
            "last_update" to this.lastUpdate,
            "actor_id" to this.id
    )

    fun insert(actor: Actor): Actor {
        return if (actor.id == 0) {
            val sql = "insert into actor(first_name, last_name, last_update) values (:first_name, :last_name, :last_update)"
            val (count, key) = session.insert(sql, actor.toMap(), UpdateOptions(useGeneratedKeys = true)) { it.resultSet.getInt(1) }
            assertEquals(1, count)
            actor.copy(id = key)
        } else {
            val sql = "insert into actor(actor_id, first_name, last_name, last_update) values (:actor_id, :first_name, :last_name, :last_update)"
            val count = session.update(sql, actor.toMap())
            assertEquals(1, count)
            actor
        }
    }

    fun deleteActor(id: Int) = session.update("delete from actor where actor_id = :id", mapOf("id" to id))

    fun maxId(id: String, table: String): Int {
        return session.select("select max($id) id from $table") { row -> row.intOrNull("id") }.first() ?: 0
    }

    fun selectActors(ids: Set<Int?> = setOf()): List<Actor> {
        val params = hashMapOf<String, Any?>()
        val sql = "select actor_id, first_name, last_name, last_update from actor " + if (ids.isEmpty()) "" else {
            params["ids"] = ids
            "where actor_id in (:ids) order by actor_id"
        }
        return session.select(sql, params) { row ->
            Actor(row.string("first_name"), row.stringOrNull("last_name"), row.int("actor_id"), row.timestamp("last_update"))
        }
    }
}