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
import com.github.andrewoma.kwery.example.film.dao.LanguageDao
import com.github.andrewoma.kwery.example.film.dao.languageTable
import com.github.andrewoma.kwery.example.film.model.Language
import com.github.andrewoma.kwery.fetcher.GraphFetcher
import com.github.andrewoma.kwery.mapper.IdStrategy
import com.github.andrewoma.kwery.transactional.jersey.Transactional
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Path("/languages")
@Produces(MediaType.APPLICATION_JSON)
@Transactional class LanguageResource(val languageDao: LanguageDao, override val fetcher: GraphFetcher) : Resource {
    @Timed @GET
    fun find(@QueryParam("name") name: String?): List<Language> {

        val filter = parameters(languageTable.Name optional name)

        return languageDao.findByExample(languageTable.copy(Language(), filter), filter.keySet())
    }

    @Timed @GET @Path("/{id}")
    fun findById(@PathParam("id") id: Int): Language {
        return languageDao.findById(id) ?: throw NotFoundException("$id not found")
    }

    @Timed @POST
    fun create(language: Language): Int {
        return languageDao.insert(language.copy(version = 1), IdStrategy.Generated).id
    }

    @Timed @PUT @Path("/{id}")
    fun update(@PathParam("id") id: Int, language: Language): Int {
        return languageDao.update(Language(id).copy(version = language.version), language).version
    }

    @Timed @DELETE @Path("/{id}")
    fun delete(@PathParam("id") id: Int) {
        if (languageDao.delete(id) == 0) throw NotFoundException("$id not found")
    }
}