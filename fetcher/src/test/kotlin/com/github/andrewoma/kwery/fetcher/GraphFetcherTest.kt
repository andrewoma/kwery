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

package com.github.andrewoma.kwery.fetcher

import org.junit.Test as test
import org.junit.Before as before
import kotlin.reflect.KMemberProperty
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import java.util.concurrent.TimeUnit
import org.junit.Ignore
import com.github.andrewoma.kommon.util.StopWatch

class GraphFetcherTest {

    val String.graph: Node
        get() = Node.parse(this)

    class TrackedType<T, ID>(tracked: Type<T, ID>) {
        val calls = arrayListOf<Iterable<ID>>()
        val fetch: (Collection<ID>) -> Map<ID, T> = { ids ->
            calls.add(ids)
            tracked.fetch(ids)
        }
        val type = Type(tracked.javaClass, tracked.id, fetch, tracked.properties)
    }

    val continent = TrackedType(Type(javaClass<Continent>(), { it.id }, { ids -> continents.filter { ids.contains(it.key) } }))
    val continentEurope = Continent("EU", "Europe")
    val continentAsia = Continent("AS", "Asia")
    val continentAustralia = Continent("AU", "Australia")
    val continents = map(Continent::id, continentEurope, continentAsia, continentAustralia)

    val country = TrackedType(Type(javaClass<Country>(), { it.id }, { ids -> countries.filter { ids.contains(it.key) } }, listOf(
            Property(Country::continent, continent.type, { it.continent?.id }, {(c, con) -> c.copy(continent = con) })
    )))

    val countryEngland = Country("GB", "England", Continent("EU"))
    val countryJapan = Country("JP", "Japan", Continent("AS"))
    val countryAustralia = Country("AU", "Germany", Continent("AU"))
    val countries = map(Country::id, countryEngland, countryJapan, countryAustralia)

    val language = TrackedType(Type(javaClass<Language>(), { it.id }, { ids -> languages.filter { ids.contains(it.key) } }, listOf(
            Property(Language::country, country.type, { it.country?.id }, {(l, c) -> l.copy(country = c) }),
            Property(Language::country2, country.type, { it.country2?.id }, {(l, c) -> l.copy(country2 = c) })
    )))

    val languageEnglish = Language("E", "English", Country("GB"), Country("JP"))
    val languageJapanese = Language("J", "Japanese", Country("JP"))
    val languageAustralian = Language("A", "Australian", Country("AU"))
    val languages = map(Language::id, languageEnglish, languageJapanese, languageAustralian)

    val actor = TrackedType(Type(javaClass<Actor>(), { it.id }, { ids -> actors.filter { ids.contains(it.key) } }, listOf(
            Property(Actor::language, language.type, { it.language.id }, {(a, l) -> a.copy(language = l) })
    )))

    val actor1 = Actor(1, "Brad", "Spit", Language("E"))
    val actor2 = Actor(2, "Kate", "Beck", Language("J"))
    val actors = map(Actor::id, actor1, actor2)

    val film1 = Film(10, "Fight Club", Language("E"), Language("J"))
    val film2 = Film(11, "Groundhog Day", Language("A"))
    val films = map(Film::id, film1, film2)

    val actorsByFilm = mapOf(
            film1.id to listOf(actor1),
            film2.id to listOf(actor1, actor2)
    )

    val filmLanguage = Property(Film::language, language.type, { it.language.id }, {(f, l) -> f.copy(language = l) })
    val filmOriginalLanguage = Property(Film::originalLanguage, language.type, { it.originalLanguage?.id }, {(f, l) -> f.copy(originalLanguage = l) })

    val filmActors = CollectionProperty(Film::actors, actor.type, { it.id }, {(f, a) -> f.copy(actors = a.toSet()) }, { ids ->
        println("Fetching actors for films: $ids")
        actorsByFilm.filter { ids.contains(it.key) }
    })

    val film = TrackedType(Type(javaClass<Film>(), { it.id }, { ids -> films.filter { ids.contains(it.key) } }, listOf(
            filmLanguage, filmOriginalLanguage, filmActors
    )))

    fun <T, ID> map(id: KMemberProperty<T, ID>, vararg objects: T): Map<ID, T> {
        return objects.map { id.get(it) to it }.toMap()
    }

    before fun setUp() {
        film.calls.clear()
        actor.calls.clear()
        language.calls.clear()
        country.calls.clear()
        continent.calls.clear()
    }

    test fun testFindMatchingType() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type))
        assertEquals(film.type, fetcher.findMatchingType(films.values().first()))
        assertEquals(actor.type, fetcher.findMatchingType(actors.values().first()))
        assertEquals(language.type, fetcher.findMatchingType(languages.values().first()))
    }

    test(expected = javaClass<IllegalArgumentException>()) fun testFindMatchingTypeRejectsUnknown() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type))
        assertEquals(film, fetcher.findMatchingType(""))
    }

    test fun testFindMatchingPropertiesAll() {
        val fetcher = GraphFetcher(setOf(film.type))
        val properties = fetcher.findMatchingProperties(film.type, Node.all)
        assertEquals(film.type.properties.size, properties.size)
        for ((property, node) in properties) {
            assertEquals(Node.all, node)
        }
    }

    test fun testFindMatchingProperties() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type))
        var properties = fetcher.findMatchingProperties(film.type, "language".graph)
        assertEquals(setOf(filmLanguage to Node("language")), properties)

        properties = fetcher.findMatchingProperties(film.type, "language, actors".graph)
        assertEquals(setOf(filmLanguage to Node("language"), filmActors to Node("actors")), properties)
    }

    test fun testFetchOptionalProperty() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type))
        val result = fetcher.fetch(setOf(film2), "originalLanguage".graph)

        assertEquals(1, result.size)
        val film = result.first!!
        assertNull(film.originalLanguage)
        assertEquals(0, language.calls.size)
    }

    test fun testFetchPropertyCombinesSameType() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type))
        val result = fetcher.fetch(setOf(film1), "language, originalLanguage".graph)

        assertEquals(1, result.size)
        val film = result.first!!
        assertEquals(languageEnglish, film.language)
        assertEquals(languageJapanese, film.originalLanguage)
        assertEquals(1, language.calls.size)
    }

    test fun testFetchPropertyCombinesMultipleObjects() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type))

        val graph = "language, originalLanguage".graph
        val result = fetcher.fetch(setOf(film1, film2), graph)

        assertEquals(2, result.size)
        val film1 = result[0]
        val film2 = result[1]
        assertEquals(languageEnglish, film1.language)
        assertEquals(languageJapanese, film1.originalLanguage)
        assertEquals(languageAustralian, film2.language)
        language.assertCalls(1)
    }

    [Ignore] test fun testMultipleLevels() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type, country.type))
        val graph = "language(country(continent))".graph

        var result: List<Film> = listOf()
        val sw = StopWatch().start()
        for (i in 1..10000) {
            result = fetcher.fetch(setOf(film1, film2), graph)
        }
        sw.stop()
        println(sw)
        println("${sw.elapsed(TimeUnit.MILLISECONDS) * 1.0 / 10000} ms/op")

        assertEquals(2, result.size)
        val film1 = result[0]
        val film2 = result[1]

        assertNotNull(film1.language.name)
        assertNotNull(film1.language.country?.name)
        assertNotNull(film1.language.country?.continent?.name)

        assertNotNull(film2.language.name)
        assertNotNull(film2.language.country?.name)
        assertNotNull(film2.language.country?.continent?.name)

        //        println(result)
        //        println(film.calls)
        //        println(actor.calls)
        //        println(language.calls)
        //        println(country.calls)
        //        println(continent.calls)

        language.assertCalls(1)
        country.assertCalls(1)
        continent.assertCalls(1)
    }

    fun TrackedType<*, *>.assertCalls(expected: Int) = assertEquals(expected, this.calls.size)

    test fun testCollectionProperties() {
        val fetcher = GraphFetcher(setOf(film.type, actor.type, language.type, country.type))
        val graph = "actors(language)".graph
        val result = fetcher.fetch(setOf(film1, film2), graph)
        for (film in result) {
            println("${film.id} ${film.name}")
            for (actor in film.actors) {
                println("\t$actor")
            }
        }

        println(film.calls)
        println(actor.calls)
        println(language.calls)
        println(country.calls)
        println(continent.calls)
    }
}

data class Actor (
        val id: Int,
        val firstName: String,
        val lastName: String,
        val language: Language
)

data class Language (
        val id: String,
        val name: String = "",
        val country: Country? = null,
        val country2: Country? = null
)

data class Country(
        val id: String,
        val name: String = "",
        val continent: Continent? = null
)

data class Continent(
        val id: String,
        val name: String = ""
)

data class Film (
        val id: Int,
        val name: String,
        val language: Language,
        val originalLanguage: Language? = null,
        val actors: Set<Actor> = setOf()
)

