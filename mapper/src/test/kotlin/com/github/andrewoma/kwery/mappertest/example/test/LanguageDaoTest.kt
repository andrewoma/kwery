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

import com.github.andrewoma.kwery.mappertest.example.Language
import com.github.andrewoma.kwery.mappertest.example.LanguageDao
import java.time.LocalDateTime
import kotlin.properties.Delegates
import org.junit.Test as test

class LanguageDaoTest : AbstractFilmDaoTest<Language, Int, LanguageDao>() {
    override var dao: LanguageDao by Delegates.notNull()

    override fun afterSessionSetup() {
        dao = LanguageDao(session)
        super<AbstractFilmDaoTest>.afterSessionSetup()
    }

    override val data = listOf(
            Language(-1, "German", LocalDateTime.now()),
            Language(-1, "Dutch", LocalDateTime.now()),
            Language(-1, "Japanese", LocalDateTime.now()),
            Language(-1, "Chinese", LocalDateTime.now())
    )

    override fun mutateContents(t: Language) = t.copy(name = "Russian")

    override fun contentsEqual(t1: Language, t2: Language) =
            t1.name == t2.name
}