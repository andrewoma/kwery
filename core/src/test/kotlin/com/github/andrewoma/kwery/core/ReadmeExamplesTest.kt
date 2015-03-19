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

package com.github.andrewoma.kwery.core

import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import org.junit.Test as test

class ReadmeExamplesTest : AbstractFilmSessionTest() {
    test fun readmeExamples() {
        val connection = hsqlDataSource.getConnection()

        class Actor(val firstName: String, val lastName: String)

        val session = DefaultSession(connection, HsqlDialect())

        val sql = "select * from actor where first_name = :first_name"

        val actors = session.select(sql, mapOf("first_name" to "Brad")) { row ->
            Actor(row.string("first_name"), row.string("last_name"))
        }

        println(actors.firstOrNull()?.firstName + actors.firstOrNull()?.lastName)
    }
}