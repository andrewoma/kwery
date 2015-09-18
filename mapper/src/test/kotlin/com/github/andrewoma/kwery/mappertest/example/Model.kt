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

import java.time.Duration
import java.time.LocalDateTime

private val defaultLocalDateTime = LocalDateTime.now()

data class Name(val firstName: String, val lastName: String)

data class Actor(val name: Name, val id: Int = -1, val lastUpdate: LocalDateTime = defaultLocalDateTime)

fun Actor(id: Int = -1) = Actor(Name("", ""), id, defaultLocalDateTime)

enum class FilmRating {
    G, PG, PG_13, R, NC_17
}

data class Film(
        val id: Int,
        val title: String,
        val releaseYear: Int?,
        val language: Language,
        val originalLanguage: Language?,
        val duration: Duration?, // minutes
        val rating: FilmRating?,
        val lastUpdate: LocalDateTime,
        val specialFeatures: List<String> = listOf(),
        val actors: Set<Actor> = setOf()
)

fun Film(id: Int = -1): Film = Film(id, "", 0, Language(-1), Language(-1), Duration.ZERO,
        FilmRating.G, defaultLocalDateTime)

data class FilmActor(
        val id: FilmActor.Id,
        val lastUpdate: LocalDateTime

) {
    data class Id(val filmId: Int, val actorId: Int)
}

data class Language(val id: Int, val name: String, val lastUpdate: LocalDateTime)

fun Language(id: Int = -1): Language = Language(id, "", defaultLocalDateTime)
