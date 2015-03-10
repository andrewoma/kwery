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

package com.github.andrewoma.kwery.example.film.model

import java.time.Duration

data class Name(val first: String, val last: String)

data class Actor(
        val id: Int,
        val name: Name,
        override val version: Int = 1,
        val films: Set<Film> = setOf()
) : AttributeSetByVersion

fun Actor(id: Int = 0) = Actor(id, Name("", ""), 0)

data class Film (
        val id: Int,
        val title: String,
        val description: String,
        val releaseYear: Int,
        val language: Language,
        val originalLanguage: Language?,
        val duration: Duration?, // minutes
        val rating: FilmRating?,
        val specialFeatures: List<String> = listOf(),
        val actors: Set<Actor> = setOf(),
        override val version: Int = 1

) : AttributeSetByVersion

fun Film(id: Int = 0): Film = Film(id, "", "", 0, Language(-1), null, Duration.ZERO, null, version = 0)

enum class FilmRating {
    G PG PG_13 R NC_17
}

data class FilmActor(val id: FilmActor.Id) {
    data class Id(val filmId: Int, val actorId: Int)
}

data class Language (
        val id: Int,
        val name: String,
        override val version: Int = 1
) : AttributeSetByVersion

fun Language(id: Int = 0): Language = Language(id, "", 0)

trait Version {
    val version: Int
}

// TODO ... move AttributeSet logic to be external to the model itself into the Jackson filter definition
enum class AttributeSet { Id All }

trait HasAttributeSet {
    fun attributeSet(): AttributeSet
}

trait AttributeSetByVersion : HasAttributeSet, Version {
    override fun attributeSet() = if (this.version == 0) AttributeSet.Id else AttributeSet.All
}
