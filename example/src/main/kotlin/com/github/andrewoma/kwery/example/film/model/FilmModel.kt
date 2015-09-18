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

data class Name(val first: String = "", val last: String = "")

data class Actor(
        val id: Int = 0,
        val name: Name = Name(),
        override val version: Int = 0,
        val films: Set<Film> = setOf()
) : AttributeSetByVersion

data class Film(
        val id: Int = 0,
        val title: String = "",
        val description: String = "",
        val releaseYear: Int = 0,
        val language: Language = Language(0),
        val originalLanguage: Language? = null,
        val duration: Duration? = null, // minutes
        val rating: FilmRating? = null,
        val specialFeatures: List<String> = listOf(),
        val actors: Set<Actor> = setOf(),
        override val version: Int = 0

) : AttributeSetByVersion

enum class FilmRating {
    G, PG, PG_13, R, NC_17
}

data class FilmActor(val id: FilmActor.Id = FilmActor.Id()) {
    data class Id(val filmId: Int = 0, val actorId: Int = 0)
}

data class Language(
        val id: Int = 0,
        val name: String = "",
        override val version: Int = 0
) : AttributeSetByVersion

interface Version {
    val version: Int
}

// TODO ... move AttributeSet logic to be external to the model itself into the Jackson filter definition
enum class AttributeSet { Id, All }

interface HasAttributeSet {
    fun attributeSet(): AttributeSet
}

interface AttributeSetByVersion : HasAttributeSet, Version {
    override fun attributeSet() = if (this.version == 0) AttributeSet.Id else AttributeSet.All
}
