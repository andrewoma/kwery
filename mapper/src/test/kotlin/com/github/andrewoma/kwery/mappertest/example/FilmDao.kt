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
import com.github.andrewoma.kwery.core.StatementOptions
import com.github.andrewoma.kwery.core.builder.query
import com.github.andrewoma.kwery.mapper.*
import java.time.Duration
import java.time.temporal.ChronoUnit
import com.github.andrewoma.kwery.mappertest.example.Film as F

object filmTable : Table<F, Int>("film", tableConfig), VersionedWithTimestamp {
    // @formatter:off
    val FilmId             by col(F::id,               id = true)
    val Title              by col(F::title)
    val ReleaseYear        by col(F::releaseYear)
    val LanguageId         by col(F::language)
    val OriginalLanguageId by col(F::originalLanguage)
    val Length             by col(F::duration,         converter = optional(DurationConverter(ChronoUnit.SECONDS)))
    val Rating             by col(F::rating)
    val LastUpdate         by col(F::lastUpdate,       version = true)
    val SpecialFeatures    by col(F::specialFeatures,  default = listOf<String>(), converter = ArrayConverter<String>("varchar"))
    // @formatter:on

    override fun idColumns(id: Int) = setOf(FilmId of id)

    override fun create(value: Value<F>): F = F(value of FilmId, value of Title,
            value of ReleaseYear, value of LanguageId, value of OriginalLanguageId,
            value of Length, value of Rating, value of LastUpdate,
            value of SpecialFeatures)
}

data class FilmCriteria(
        val ratings: Set<FilmRating> = setOf(),
        val title: String? = null,
        val releaseYear: Int? = null,
        val maxDuration: Duration? = null,
        val actor: Actor? = null,
        val limit: Int? = null,
        val offset: Int? = null
)

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

        val params = filmTable.objectMap(session, F().copy(title = title, releaseYear = releaseYear))

        return session.select(sql, params, StatementOptions("findWithActors"), combine(f.mapper, a.mapper))
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
        val params = filmTable.objectMap(session, F().copy(title = title, releaseYear = releaseYear))

        return session.select(sql, params, StatementOptions("findWithLanguages")) { row ->
            f.mapper(row).copy(language = l.mapper(row), originalLanguage = ol.optionalMapper(row))
        }.firstOrNull()
    }

    // An example of a partial select (title and release year only)
    fun findAllTitlesAndReleaseYears() = findAll(setOf(filmTable.Title, filmTable.ReleaseYear))

    // An example of using a QueryBuilder to build a dynamic query
    fun findByCriteria(criteria: FilmCriteria): List<F> {
        val query = query {
            select("select ${f.select} from film f")
            whereGroup {
                criteria.title?.let {
                    where("lower(f.title) like :title")
                    parameter("title", "%${it.toLowerCase()}%")
                }
                criteria.releaseYear?.let {
                    where("f.release_year = :release_year")
                    parameter("release_year", it)
                }
                criteria.actor?.let {
                    where("exists (select 1 from film_actor where film_id = f.film_id and actor_id = :actor_id)")
                    parameter("actor_id", it.id)
                }
                criteria.maxDuration?.let {
                    where("(f.length is null or f.length <= :max_length)")
                    parameter("max_length", filmTable.Length.converter.to(session.connection, it))
                }
                if (criteria.ratings.isNotEmpty()) {
                    where("f.rating in (:ratings)")
                    parameter("ratings", criteria.ratings.map { filmTable.Rating.converter.to(session.connection, it) })
                }
            }
            orderBy("title, release_year")
        }

        return session.select(query.sql, query.parameters, StatementOptions("findByCriteria", limit = criteria.limit,
                offset = criteria.offset), mapper = f.mapper)
    }
}
