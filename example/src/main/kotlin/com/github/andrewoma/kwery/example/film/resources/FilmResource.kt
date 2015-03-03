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

package com.github.andrewoma.kwery.example.film.resources

import com.codahale.metrics.annotation.Timed as timed

import javax.ws.rs.GET as get
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam as param
import javax.ws.rs.core.MediaType
import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.example.film.jersey.transaction

Path("/films")
Produces(MediaType.APPLICATION_JSON)
public class FilmResource(val session: Session) {

    transaction timed get fun findFilms(param("name") name: String?): String {
        val count = session.select("select count(*) c from actor") { row -> row.long("c")}.single()
        return "Hello ${name ?: "Unknown"}, there are $count actors"
    }
}