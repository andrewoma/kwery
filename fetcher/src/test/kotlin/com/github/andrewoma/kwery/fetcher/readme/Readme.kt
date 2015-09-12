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

package com.github.andrewoma.kwery.fetcher.readme

import com.github.andrewoma.kwery.fetcher.*
import org.junit.Test

// Given the following domain model
data class Actor(val id: Int, val firstName: String, val lastName: String)

data class Language(val id: Int, val name: String)

data class Film(val id: Int, val language: Language, val actors: Set<Actor>,
                val title: String, val releaseYear: Int)

@suppress("UNUSED_PARAMETER")
class Dao<ID, T> {
    fun findByIds(id: Collection<ID>): Map<ID, T> = mapOf()
    fun findByFilmIds(id: Collection<ID>): Map<ID, Collection<T>> = mapOf()
    fun findFilmsReleasedAfter(year: Int): List<Film> = listOf()
}

fun readme() {
    val languageDao = Dao<Int, Language>()
    val filmDao = Dao<Int, Film>()
    val actorDao = Dao<Int, Actor>()

    // Define types with functions describing how to fetch a batch by ids
    val language = Type(Language::id, { languageDao.findByIds(it) })
    val actor = Type(Actor::id, { actorDao.findByIds(it) })

    // For types that reference other types describe how to apply fetched values
    val film = Type(Film::id, { filmDao.findByIds(it) }, listOf(
            // 1 to 1
            Property(Film::language, language, { it.language.id }, { f, l -> f.copy(language = l) }),

            // 1 to many requires a function to describe how to fetch the related objects
            CollectionProperty(Film::actors, actor, { it.id },
                    { f, a -> f.copy(actors = a.toSet()) },
                    { actorDao.findByFilmIds(it) })
    ))

    val fetcher = GraphFetcher(setOf(language, actor, film))

    // Extension function to fetch the graph for any List using fetcher defined above
    fun <T> List<T>.fetch(vararg nodes: Node) = fetcher.fetch(this, Node.create("", nodes.toSet()))

    // We can now efficiently fetch various graphs for any list of films
    // The following fetches the films with actors and languages in 3 queries
    val filmsWithAll = filmDao.findFilmsReleasedAfter(2010).fetch(Node.all)

    // The graph specification can also be built using properties
    val filmsWithActors = filmDao.findFilmsReleasedAfter(2010).fetch(Film::actors.node())

    println("$filmsWithAll $filmsWithActors")
}

fun main(args: Array<String>) {
    readme()
}