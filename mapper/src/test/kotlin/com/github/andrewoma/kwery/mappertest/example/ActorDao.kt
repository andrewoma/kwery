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
import com.github.andrewoma.kwery.mapper.AbstractDao
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.Value
import com.github.andrewoma.kwery.mapper.VersionedWithTimestamp
import com.github.andrewoma.kwery.mappertest.example.Actor as A
import com.github.andrewoma.kwery.mappertest.example.Name as N

object actorTable : Table<A, Int>("actor", tableConfig, "actor_seq"), VersionedWithTimestamp {
    // @formatter:off
    val ActorId    by col(A::id,                     id = true)
    val FirstName  by col(N::firstName, { it.name })
    val LastName   by col(N::lastName,  { it.name })
    val LastUpdate by col(A::lastUpdate,             version = true)
    // @formatter:on

    override fun idColumns(id: Int) = setOf(ActorId of id)

    override fun create(value: Value<A>) = A(N(value.of(FirstName), value.of(LastName)),
            value.of(ActorId), value.of(LastUpdate))
}

class ActorDao(session: Session, val filmActorDao: FilmActorDao) :
        AbstractDao<A, Int>(session, actorTable, { it.id }, "int", defaultId = -1) {

    fun findByLastNames(lastNames: List<String>): List<A> {
        val sql = "select $columns from ${table.name} where last_name in (:last_names)"
        val parameters = mapOf("last_names" to lastNames)
        return session.select(sql, parameters, options("findByLastNames"), table.rowMapper())
    }

    fun findByFilmIds(ids: Collection<Int>): Map<Int, Collection<A>> {
        val filmActors = filmActorDao.findByFilmIds(ids)
        val films = findByIds(filmActors.map { it.id.actorId }.toSet())
        return filmActors.groupBy { it.id.filmId }.mapValues { it.value.map { films[it.id.actorId]!! } }
    }
}
