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
import com.github.andrewoma.kwery.fetcher.*

fun createFilmFetcher(session: Session): GraphFetcher {
    val languageDao = LanguageDao(session)
    val filmActorDao = FilmActorDao(session)
    val actorDao = ActorDao(session, filmActorDao)
    val filmDao = FilmDao(session)

    val language = Type(Language::id, { languageDao.findByIds(it) })
    val actor = Type(Actor::id, { actorDao.findByIds(it) })

    val film = Type(Film::id, { filmDao.findByIds(it) }, listOf(
            Property(Film::language, language, { it.language.id }, {(f, l) -> f.copy(language = l) }),
            Property(Film::originalLanguage, language, { it.originalLanguage?.id }, {(f, l) -> f.copy(originalLanguage = l) }),
            CollectionProperty(Film::actors, actor, { it.id },
                    {(f, a) -> f.copy(actors = a.toSet()) },
                    { actorDao.findByFilmIds(it) })
    ))

    return GraphFetcher(setOf(language, actor, film))
}

