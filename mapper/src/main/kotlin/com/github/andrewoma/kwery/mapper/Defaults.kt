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

package com.github.andrewoma.kwery.mapper

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

public val standardDefaults: Map<Class<*>, *> = listOf(
        reifiedValue(true),
        reifiedValue(0.toByte()),
        reifiedValue(0.toChar()),
        reifiedValue(0.toShort()),
        reifiedValue(0),
        reifiedValue(0L),
        reifiedValue(0.toFloat()),
        reifiedValue(0.toDouble()),
        reifiedValue(BigDecimal(0)),
        reifiedValue("")
).toMap()

public val timeDefaults: Map<Class<*>, *> = listOf(
        reifiedValue(LocalDate.now()),
        reifiedValue(LocalDateTime.now()),
        reifiedValue(Duration.ZERO),
        reifiedValue(ZonedDateTime.now())
).toMap()

inline public fun <reified T> reifiedValue(default: T): Pair<Class<T>, T> = javaClass<T>() to default