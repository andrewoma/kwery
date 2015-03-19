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

package com.github.andrewoma.kwery.example.film.dao

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.mapper.AbstractDao
import com.github.andrewoma.kwery.mapper.Table
import com.github.andrewoma.kwery.mapper.Value
import com.github.andrewoma.kwery.mapper.VersionedWithInt
import com.github.andrewoma.kwery.example.film.model.Language as L

object languageTable : Table<L, Int>("language", tableConfig), VersionedWithInt {
    // @formatter:off
    val Id      by col(L::id,      id = true)
    val Name    by col(L::name,    notNull = true)
    val Version by col(L::version, version = true)
    // @formatter:on

    override fun idColumns(id: Int) = setOf(Id of id)
    override fun create(value: Value<L>) = L(value.of(Id), value.of(Name), value.of(Version))
}

class LanguageDao(session: Session) : AbstractDao<L, Int>(session, languageTable, { it.id }, "int", defaultId = 0)
