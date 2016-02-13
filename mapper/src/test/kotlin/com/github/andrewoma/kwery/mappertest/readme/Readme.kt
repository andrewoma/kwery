/*
 * Copyright (c) 2016 Andrew O'Malley
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

package com.github.andrewoma.kwery.mappertest.readme

import com.github.andrewoma.kwery.mappertest.AbstractSessionTest
import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.mapper.*
import org.junit.Test
import kotlin.test.assertEquals

data class Film(
        val id: Int = 0,
        val language: Language = Language(0),
        val originalLanguage: Language? = null
)

data class Language(
        val id: Int = 0,
        val name: String = "")


// Option 1: Uses paths and constructors -------------------------------------------------------------------------------

object filmTable1 : Table<Film, Int>("readme_film") {
    // @formatter:off
    val Id                 by col(Film::id, id = true)
    val LanguageId         by col(Language::id, Film::language)
    val OriginalLanguageId by optionalCol(Language::id, Film::originalLanguage)
    // @formatter:on

    override fun idColumns(id: Int) = setOf(Id of id)

    override fun create(value: Value<Film>): Film =
            Film(value of Id, Language(value of LanguageId), (value of OriginalLanguageId)?.let { Language(it) })
}

class FilmDao1(session: Session) : AbstractDao<Film, Int>(session, filmTable1, Film::id, "int", defaultId = 0)

// Option 2: Add defaults and converters for you type ------------------------------------------------------------------

object languageConverter : SimpleConverter<Language>(
        { row, c -> Language(row.int(c)) },
        Language::id
)

val tableConfig = TableConfiguration(
        defaults = standardDefaults + reifiedValue(Language(0)),
        converters = standardConverters + reifiedConverter(languageConverter)
)

object filmTable2 : Table<Film, Int>("readme_film", tableConfig) {
    // @formatter:off
    val Id                 by col(Film::id, id = true)
    val LanguageId         by col(Film::language)
    val OriginalLanguageId by col(Film::originalLanguage)
    // @formatter:on

    override fun idColumns(id: Int) = setOf(Id of id)

    override fun create(value: Value<Film>): Film =
            Film(value of Id, value of LanguageId, value of OriginalLanguageId)
}

class FilmDao2(session: Session) : AbstractDao<Film, Int>(session, filmTable2, Film::id, "int", defaultId = 0)

// Test ----------------------------------------------------------------------------------------------------------------

abstract class ReadmeTest : AbstractSessionTest() {
    lateinit var dao: AbstractDao<Film, Int>

    override fun afterSessionSetup() {
        initialise("readmeSchema") {
            session.update("""
                create table readme_film (
                    id                   integer identity,
                    language_id          integer not null,
                    original_language_id integer
                )"""
            )
        }
        session.update("delete from readme_film")
    }

    @Test fun `should round trip`() {
        val film = Film(1, Language(1), Language(2))
        dao.insert(film)
        assertEquals(film, dao.findById(film.id))
    }

    @Test fun `should round trip with optional null`() {
        val film = Film(1, Language(1), null)
        dao.insert(film)
        assertEquals(film, dao.findById(film.id))
    }
}

class Readme1Test : ReadmeTest() {
    override fun afterSessionSetup() {
        super.afterSessionSetup()
        dao = FilmDao1(session)
    }
}

class Readme2Test : ReadmeTest() {
    override fun afterSessionSetup() {
        super.afterSessionSetup()
        dao = FilmDao2(session)
    }
}
