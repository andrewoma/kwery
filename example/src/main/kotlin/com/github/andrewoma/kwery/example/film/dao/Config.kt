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

import com.github.andrewoma.kwery.example.film.model.FilmRating
import com.github.andrewoma.kwery.example.film.model.Language
import com.github.andrewoma.kwery.mapper.*
import com.github.andrewoma.kwery.mapper.util.camelToLowerUnderscore
import kotlin.reflect.KType

val domainDefaults: Map<KType, *> = listOf(
        reifiedValue(FilmRating.G),
        reifiedValue(Language(-1))
).toMap()

val defaults: Map<KType, *> = standardDefaults + timeDefaults + domainDefaults

val domainConverters: Map<Class<*>, Converter<*>> = listOf(
        reifiedConverter(filmRatingConverter),
        reifiedConverter(languageConverter)
).toMap()

val converters: Map<Class<*>, Converter<*>> = standardConverters + timeConverters + domainConverters

object languageConverter : SimpleConverter<Language>(
        { row, c -> Language(row.int(c)) },
        { it.id }
)

object filmRatingConverter : SimpleConverter<FilmRating>(
        { row, c -> FilmRating.valueOf(row.string(c).replace('-', '_')) },
        { it.name().replace('_', '-') }
)

val tableConfig = TableConfiguration(defaults, converters, camelToLowerUnderscore)
