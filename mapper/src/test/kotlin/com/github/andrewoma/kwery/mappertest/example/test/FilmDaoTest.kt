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

import com.github.andrewoma.kwery.fetcher.GraphFetcher
import com.github.andrewoma.kwery.fetcher.Node
import com.github.andrewoma.kwery.fetcher.node
import com.github.andrewoma.kwery.mappertest.example.*
import java.time.Duration
import java.time.LocalDateTime
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class FilmDaoTest : AbstractFilmDaoTest<Film, Int, FilmDao>() {
    var graphFetcher: GraphFetcher by Delegates.notNull()
    var filmActorDao: FilmActorDao by Delegates.notNull()

    override var dao: FilmDao by Delegates.notNull()

    override fun afterSessionSetup() {
        dao = FilmDao(session)
        filmActorDao = FilmActorDao(session)
        graphFetcher = createFilmFetcher(session)
        super<AbstractFilmDaoTest>.afterSessionSetup()
    }

    override val data by Delegates.lazy {
        listOf(
                Film(-1, "Underworld", 2003, sd.languageEnglish, null, Duration.ofMinutes(121), FilmRating.NC_17,
                        LocalDateTime.now(), listOf("Commentaries", "Behind the Scenes")),
                Film(-1, "Underworld: Evolution", 2006, sd.languageEnglish, null, Duration.ofMinutes(106), FilmRating.R,
                        LocalDateTime.now(), listOf("Behind the Scenes")),
                Film(-1, "Underworld: Rise of the Lycans", 2006, sd.languageEnglish, sd.languageSpanish, Duration.ofMinutes(92), FilmRating.R,
                        LocalDateTime.now(), listOf())
        )
    }

    override fun mutateContents(t: Film) = t.copy(
            title = "Resident Evil",
            language = sd.languageSpanish,
            originalLanguage = sd.languageEnglish,
            duration = Duration.ofMinutes(10),
            rating = FilmRating.G
    )

    override fun contentsEqual(t1: Film, t2: Film) =
            t1.title == t2.title && t1.language.id == t2.language.id && t1.originalLanguage?.id == t2.originalLanguage?.id &&
                    t1.duration == t2.duration && t1.rating == t2.rating

    @Test fun `findWithActors should fetch actors via a join`() {
        val film = insertAll().first()

        val actors = listOf(sd.actorKate, sd.actorBrad)
        for (actor in actors) {
            filmActorDao.insert(FilmActor(FilmActor.Id(film.id, actor.id), LocalDateTime.now()))
        }

        val found = dao.findWithActors(film.title, film.releaseYear)!!
        assertEquals(actors.toSet(), found.actors)
    }

    @Test fun `findWithLanguages should fetch actors via a join`() {
        val films = insertAll()

        dao.findWithLanguages(films[0].title, films[0].releaseYear)!!.let { film ->
            assertEquals(film.language, sd.languageEnglish)
            assertNull(film.originalLanguage)
        }

        dao.findWithLanguages(films[2].title, films[2].releaseYear)!!.let { film ->
            assertEquals(film.language, sd.languageEnglish)
            assertEquals(film.originalLanguage, sd.languageSpanish)
        }
    }

    @Test fun `findAllTitlesAndReleaseYears should return partial data`() {
        insertAll()
        val films = dao.findAllTitlesAndReleaseYears()
        assertTrue(films.map { it.title }.toSet().contains("Underworld"))
        assertTrue(films.map { it.releaseYear }.toSet().contains(2006))
        assertFalse(films.map { it.rating }.toSet().contains(FilmRating.R))
    }

    fun <T> Collection<T>.fetch(node: Node) = graphFetcher.fetch(this, node)

    @Test fun `find by graph fetcher`() {
        val films = insertAll()
        val ids = films.map { it.id }

        val actors = listOf(sd.actorKate, sd.actorBrad)
        for (film in films) {
            for (actor in actors) {
                filmActorDao.insert(FilmActor(FilmActor.Id(film.id, actor.id), LocalDateTime.now()))
            }
        }

        // Check to see actors and language are populated
        var fetched = dao.findByIds(ids).values().fetch(Node(Node.all))
        assertEquals(films.size(), fetched.size())

        for (film in fetched) {
            assertEquals(2, film.actors.size())
            assertEquals(sd.languageEnglish.name, film.language.name)
            if (film.title == "Underworld: Rise of the Lycans") {
                assertEquals(sd.languageSpanish, film.originalLanguage)
            } else {
                assertNull(film.originalLanguage)
            }
        }

        // Check to see only the language is populate
        fetched = dao.findByIds(ids).values().fetch(Node(Film::language.node()))
        assertEquals(films.size(), fetched.size())

        for (film in fetched) {
            assertEquals(0, film.actors.size())
            assertEquals(sd.languageEnglish.name, film.language.name)
        }
    }
}
