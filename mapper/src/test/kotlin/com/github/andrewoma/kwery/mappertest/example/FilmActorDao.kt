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

import com.github.andrewoma.kwery.mapper.*
import com.github.andrewoma.kwery.core.*

import com.github.andrewoma.kwery.mappertest.example.FilmActor as FA

object filmActorTable : Table<FA, FA.Id>("film_actor", tableConfig), VersionedWithTimestamp {
    // @formatter:off
    val FilmId     by col(FA.Id::filmId,  path = { it.id }, id = true)
    val ActorId    by col(FA.Id::actorId, path = { it.id }, id = true)
    val LastUpdate by col(FA::lastUpdate, version = true)
    // @formatter:on

    override fun idColumns(id: FA.Id) = setOf(FilmId of id.filmId, ActorId of id.actorId)
    override fun create(value: Value<FA>) = FA(FA.Id(value of FilmId, value of ActorId), value of LastUpdate)
}

class FilmActorDao(session: Session) : AbstractDao<FA, FA.Id>(session, filmActorTable, { it.id }, KeyStrategy.Explicit) {

    fun findByFilmIds(ids: Collection<Int>): List<FilmActor> {
        val name = "findByFilmIds"
        val sql = sql(name) { "select $columns from ${table.name} where film_id in (:ids)" }
        return session.select(sql, mapOf("ids" to ids), selectOptions(name), table.rowMapper())
    }
}


