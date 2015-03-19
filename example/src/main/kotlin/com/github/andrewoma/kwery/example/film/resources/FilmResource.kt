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

import com.codahale.metrics.annotation.Timed
import com.github.andrewoma.kwery.example.film.dao.FilmDao
import com.github.andrewoma.kwery.example.film.dao.filmTable
import com.github.andrewoma.kwery.example.film.jersey.Transaction
import com.github.andrewoma.kwery.example.film.model.Film
import com.github.andrewoma.kwery.example.film.model.FilmRating
import com.github.andrewoma.kwery.fetcher.GraphFetcher
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


Path("/films")
Produces(MediaType.APPLICATION_JSON)
public class FilmResource(val filmDao: FilmDao, override val fetcher: GraphFetcher) : Resource {
    Transaction Timed GET
    fun find(QueryParam("title") title: String?,
             QueryParam("releaseYear") releaseYear: Int?,
             QueryParam("rating") rating: FilmRating?,
             QueryParam("fetch") root: String?): List<Film> {

        val filter = parameters(
                filmTable.Title + title,
                filmTable.ReleaseYear + releaseYear,
                filmTable.Rating + rating
        )

        return filmDao.findByExample(filmTable.copy(Film(), filter), filter.keySet()).fetch(root)
    }

    Transaction Timed GET Path("/{id}")
    fun findById(PathParam("id") id: Int, QueryParam("fetch") root: String?): Film {
        return filmDao.findById(id).fetch(root) ?: throw NotFoundException("$id not found")
    }
}