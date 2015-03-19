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

package com.github.andrewoma.kwery.example.film.dao

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.example.film.model.Actor
import com.github.andrewoma.kwery.example.film.model.FilmActor
import com.github.andrewoma.kwery.mapper.AbstractDao
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.Value
import com.github.andrewoma.kwery.mapper.VersionedWithInt
import com.github.andrewoma.kwery.example.film.model.Actor as A
import com.github.andrewoma.kwery.example.film.model.Name as N


object actorTable : Table<A, Int>("actor", tableConfig, "actor_seq"), VersionedWithInt {
    // @formatter:off
    val Id         by col(A::id,                 id = true)
    val FirstName  by col(N::first, { it.name }, notNull = true)
    val LastName   by col(N::last,  { it.name }, notNull = true)
    val Version    by col(A::version,            version = true)
    // @formatter:on

    override fun idColumns(id: Int) = setOf(Id of id)

    override fun create(value: Value<A>) = A(value.of(Id), N(value.of(FirstName), value.of(LastName)),
            value.of(Version))
}

class ActorDao(session: Session) : AbstractDao<A, Int>(session, actorTable, { it.id }, "int", defaultId = 0) {

    fun findByFilmIds(filmActors: Collection<FilmActor>): Map<Int, Collection<Actor>> {
        val films = findByIds(filmActors.map { it.id.actorId }.toSet())
        return filmActors.groupBy { it.id.filmId }.mapValues { it.getValue().map { films.get(it.id.actorId)!! } }
    }
}