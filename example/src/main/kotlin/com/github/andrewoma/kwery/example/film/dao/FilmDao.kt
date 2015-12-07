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
import com.github.andrewoma.kwery.example.film.model.Film
import com.github.andrewoma.kwery.example.film.model.FilmActor
import com.github.andrewoma.kwery.mapper.*
import java.time.temporal.ChronoUnit
import com.github.andrewoma.kwery.example.film.model.Film as F

object filmTable : Table<F, Int>("film", tableConfig), VersionedWithInt {
    // @formatter:off
    val Id                 by col(F::id,               id = true)
    val Title              by col(F::title)
    val Description        by col(F::description)
    val ReleaseYear        by col(F::releaseYear)
    val LanguageId         by col(F::language)
    val OriginalLanguageId by col(F::originalLanguage)
    val Length             by col(F::duration,         converter = optional(DurationConverter(ChronoUnit.SECONDS)))
    val Rating             by col(F::rating)
    val Version            by col(F::version,          version = true)
    val SpecialFeatures    by col(F::specialFeatures,  default = listOf<String>(), converter = ArrayConverter<String>("varchar"))
    // @formatter:on

    override fun idColumns(id: Int) = setOf(Id of id)

    override fun create(value: Value<F>): F = F(value of Id, value of Title, value of Description,
            value of ReleaseYear, value of LanguageId, value of OriginalLanguageId,
            value of Length, value of Rating, value of SpecialFeatures, version = value of Version)
}

class FilmDao(session: Session) : AbstractDao<F, Int>(session, filmTable, { it.id }, "int", defaultId = 0) {

    fun findByActorIds(filmActors: Collection<FilmActor>): Map<Int, Collection<Film>> {
        val films = findByIds(filmActors.map { it.id.filmId }.toSet())
        return filmActors.groupBy { it.id.filmId }.mapValues { it.value.map { films[it.id.filmId]!! } }
    }
}