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

import com.github.andrewoma.kwery.mappertest.example.*
import java.time.LocalDateTime
import kotlin.properties.Delegates

class FilmActorDaoTest : AbstractFilmDaoTest<FilmActor, FilmActor.Id, FilmActorDao>() {
    override var dao: FilmActorDao by Delegates.notNull()

    override fun afterSessionSetup() {
        dao = FilmActorDao(session)
        super<AbstractFilmDaoTest>.afterSessionSetup()
        dao.session.update("delete from ${dao.table.name}")
    }

    override val data by Delegates.lazy {
        listOf(
                FilmActor(FilmActor.Id(sd.actorKate.id, sd.filmUnderworld.id), LocalDateTime.now()),
                FilmActor(FilmActor.Id(sd.actorKate.id, sd.filmUnderworld2.id), LocalDateTime.now()),
                FilmActor(FilmActor.Id(sd.actorBrad.id, sd.filmUnderworld.id), LocalDateTime.now()),
                FilmActor(FilmActor.Id(sd.actorBrad.id, sd.filmUnderworld2.id), LocalDateTime.now())
        )
    }

    override fun mutateContents(t: FilmActor) = t

    override fun contentsEqual(t1: FilmActor, t2: FilmActor) = true
}