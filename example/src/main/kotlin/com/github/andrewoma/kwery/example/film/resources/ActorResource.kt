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
import com.github.andrewoma.kwery.example.film.dao.ActorDao
import com.github.andrewoma.kwery.example.film.dao.actorTable
import com.github.andrewoma.kwery.example.film.model.Actor
import com.github.andrewoma.kwery.fetcher.GraphFetcher
import com.github.andrewoma.kwery.transactional.jersey.transactional
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

Path("/actors")
Produces(MediaType.APPLICATION_JSON)
transactional public class ActorResource(val actorDao: ActorDao, override val fetcher: GraphFetcher) : Resource {

    Timed GET
    fun find(QueryParam("firstName") firstName: String?,
             QueryParam("lastName") lastName: String?,
             QueryParam("fetch") root: String?): List<Actor> {

        val filter = parameters(
                actorTable.FirstName optional firstName,
                actorTable.LastName optional lastName
        )

        return actorDao.findByExample(actorTable.copy(Actor(), filter), filter.keySet()).fetch(root)
    }

    Timed GET Path("/{id}")
    fun findById(PathParam("id") id: Int, QueryParam("fetch") root: String?): Actor {
        return actorDao.findById(id).fetch(root) ?: throw NotFoundException("$id not found")
    }
}
