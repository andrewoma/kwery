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

import com.github.andrewoma.kwery.mappertest.example.Film as F
import java.time.temporal.ChronoUnit

object filmTable : Table<F, Int>("film", tableConfig), VersionedWithTimestamp {
    // @formatter:off
    val FilmId             by col(F::id,               id = true)
    val Title              by col(F::title,            notNull = true)
    val ReleaseYear        by col(F::releaseYear)
    val LanguageId         by col(F::language,         notNull = true)
    val OriginalLanguageId by col(F::originalLanguage)
    val Length             by col(F::duration,         converter = optional(DurationConverter(ChronoUnit.MILLIS)))
    val Rating             by col(F::rating,           notNull = true)
    val LastUpdate         by col(F::lastUpdate,       version = true)
    val SpecialFeatures    by col(F::specialFeatures,  default = listOf<String>(), converter = ArrayConverter<String>("varchar"))
    // @formatter:on

    override fun idColumns(id: Int) = setOf(FilmId of id)

    override fun create(value: Value<F>): F = F(value of FilmId, value of Title,
            value of ReleaseYear, value of LanguageId, value of OriginalLanguageId,
            value of Length, value of Rating, value of LastUpdate,
            value of SpecialFeatures)
}

class FilmDao(session: Session) : AbstractDao<F, Int>(session, filmTable, { it.id }, "int", defaultId = -1) {
    val f = filmTable.prefixed("f")
    val a = actorTable.prefixed("a")
    val l = languageTable.prefixed("l")
    val ol = languageTable.prefixed("ol")

    // An example of a one to many join, fetching actors for a film via a join
    fun findWithActors(title: String, releaseYear: Int?): F? {
        //language=SQL
        val sql = """
            select
              ${f.select},
              ${a.select}
            from film f
              join film_actor fa on fa.film_id = f.film_id
              join actor a on a.actor_id = fa.actor_id
            where title = :title and release_year = :release_year
        """

        val params = filmTable.objectMap(session, Film().copy(title = title, releaseYear = releaseYear))

        return session.select(sql, params, SelectOptions("findWithActors"), combine(f.mapper, a.mapper))
                .join { film, actors -> film.copy(actors = actors.toSet()) }
                .firstOrNull()
    }

    // An example of a one to one joins, including outer joins
    fun findWithLanguages(title: String, releaseYear: Int?): F? {
        //language=SQL
        val sql = """
            select
              ${f.select},
              ${l.select},
              ${ol.select}
            from film f
              join language l on l.language_id = f.language_id
              left join language ol on ol.language_id = f.original_language_id
            where title = :title and release_year = :release_year
        """
        val params = filmTable.objectMap(session, Film().copy(title = title, releaseYear = releaseYear))

        return session.select(sql, params, SelectOptions("findWithLanguages")) { row ->
            f.mapper(row).copy(language = l.mapper(row), originalLanguage = ol.optionalMapper(row))
        }.firstOrNull()
    }

    // An example of a partial select (title and release year only)
    fun findAllTitlesAndReleaseYears() = findAll(setOf(filmTable.Title, filmTable.ReleaseYear))
}
