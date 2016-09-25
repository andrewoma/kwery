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

package com.github.andrewoma.kwery.example.film

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.ClassRule
import org.junit.Test
import javax.ws.rs.client.ClientBuilder
import kotlin.test.assertEquals

class FilmApplicationAcceptanceTest {
    companion object {
        val rule: DropwizardAppRule<FilmConfiguration> =
                DropwizardAppRule(FilmApplication::class.java, ResourceHelpers.resourceFilePath("dev.yml"))

        @ClassRule @JvmStatic fun aRule() = rule
    }

    fun target(url: String) = ClientBuilder.newClient().target("http://localhost:${rule.localPort}$url")

    @Test fun `Actors should find Scarlett`() {
        val response = target("/api/actors")
                .queryParam("firstName", "Scarlett")
                .queryParam("lastName", "Damon")
                .request().get(String::class.java)

        val expected = """
            [ {
              "id" : 81,
              "name" : {
                "first" : "Scarlett",
                "last" : "Damon"
              },
              "version" : 1,
              "films" : [ ]
            } ]
        """
        assertEquals(expected.trimIndent(), response)
    }

    @Test fun `Languages should find English`() {

        val response = target("/api/languages")
                .queryParam("name", "English")
                .request().get(String::class.java)

        val expected = """
            [ {
              "id" : 1,
              "name" : "English",
              "version" : 1
            } ]
        """
        assertEquals(expected.trimIndent(), response)
    }

    @Test fun `Films should find Ace Goldfinger`() {

        val response = target("/api/films")
                .queryParam("title", "Ace Goldfinger")
                .request().get(String::class.java)

        val expected = """
            [ {
              "id" : 2,
              "title" : "Ace Goldfinger",
              "description" : "A Astounding Epistle of a Database Administrator And a Explorer who must Find a Car in Ancient China",
              "releaseYear" : 2006,
              "language" : {
                "id" : 1
              },
              "originalLanguage" : null,
              "duration" : 2880.000000000,
              "rating" : "G",
              "specialFeatures" : [ "Trailers", "Deleted Scenes" ],
              "actors" : [ ],
              "version" : 1
            } ]
        """
        assertEquals(expected.trimIndent(), response)
    }
}