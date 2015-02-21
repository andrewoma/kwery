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

package com.github.andrewoma.kwery.mappertest.example.test

import com.github.andrewoma.kwery.mappertest.example.Actor
import kotlin.properties.Delegates
import com.github.andrewoma.kwery.mappertest.example.ActorDao
import com.github.andrewoma.kwery.mappertest.example.Name
import java.time.LocalDateTime
import com.github.andrewoma.kwery.mappertest.example.Language
import com.github.andrewoma.kwery.mappertest.example.LanguageDao
import com.github.andrewoma.kwery.mappertest.example.Film
import com.github.andrewoma.kwery.mappertest.example.FilmDao
import java.time.Duration
import com.github.andrewoma.kwery.mappertest.example.FilmRating
import com.github.andrewoma.kwery.mapper.AbstractDao
import com.github.andrewoma.kwery.mappertest.example.FilmActorDao

abstract class AbstractFilmDaoTest<T, ID, D : AbstractDao<T, ID>> : AbstractDaoTest<T, ID, D>() {
    class object {
        fun <T> notNull() = Delegates.notNull<T>()

        object d {
            var actorBrad: Actor by notNull()
            var actorKate: Actor by notNull()

            var languageEnglish: Language by notNull()
            var languageSpanish: Language by notNull()

            var filmUnderworld: Film by notNull()
            var filmUnderworld2: Film by notNull()
        }
    }

    val sd = d

    override fun afterSessionSetup() {
        initialise("AbstractFilmDaoTest") {
            //language=SQL
            val sql = """
            CREATE TABLE actor (
                actor_id INTEGER IDENTITY,
                first_name CHARACTER VARYING(255) NOT NULL,
                last_name CHARACTER VARYING(255) NULL,
                last_update TIMESTAMP NOT NULL
            );

            CREATE TABLE film (
                film_id INTEGER IDENTITY,
                title CHARACTER VARYING(255) NOT NULL,
                release_year INTEGER,
                language_id INTEGER NOT NULL ,
                original_language_id INTEGER,
                LENGTH INTEGER,
                rating CHARACTER VARYING (255),
                last_update TIMESTAMP NOT NULL,
                special_features VARCHAR(255) ARRAY
            );

            CREATE TABLE language (
                language_id INTEGER IDENTITY,
                name CHARACTER VARYING(255) NOT NULL,
                last_update TIMESTAMP NOT NULL
            );

            CREATE TABLE film_actor (
                film_id INTEGER NOT NULL,
                actor_id INTEGER NOT NULL,
                last_update TIMESTAMP NOT NULL,
                PRIMARY KEY(film_id, actor_id)
            )
        """

            //language=Kotlin
            for (statement in sql.split(";")) {
                session.update(statement)
            }

            val actorDao = ActorDao(session, FilmActorDao(session))
            d.actorBrad = actorDao.insert(Actor(Name("Brad", "Pitt"), -1000, LocalDateTime.now()), generateKeys = false)
            d.actorKate = actorDao.insert(Actor(Name("Kate", "Beckinsale"), -1001, LocalDateTime.now()), generateKeys = false)

            val languageDao = LanguageDao(session)
            d.languageEnglish = languageDao.insert(Language(-2000, "English", LocalDateTime.now()))
            d.languageSpanish = languageDao.insert(Language(-2001, "Spanish", LocalDateTime.now()))

            val filmDao = FilmDao(session)
            d.filmUnderworld = filmDao.insert(Film(-1, "Underworld", 2003, sd.languageEnglish, null, Duration.ofMinutes(121),
                    FilmRating.NC_17, LocalDateTime.now(), listOf("Commentaries", "Behind the Scenes")))
            d.filmUnderworld2 = filmDao.insert(Film(-1, "Underworld: Evolution", 2006, sd.languageEnglish, null,
                    Duration.ofMinutes(106), FilmRating.R, LocalDateTime.now(), listOf("Behind the Scenes")))
        }

        super.afterSessionSetup()
    }
}