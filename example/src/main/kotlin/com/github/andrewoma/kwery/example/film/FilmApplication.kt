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

import com.codahale.metrics.health.HealthCheck

import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.fasterxml.jackson.module.kotlin.KotlinModule

import com.github.andrewoma.kommon.collection.chunked

import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.github.andrewoma.kwery.core.ThreadLocalSession

import com.github.andrewoma.kwery.example.film.dao.*
import com.github.andrewoma.kwery.example.film.jackson.*
import com.github.andrewoma.kwery.example.film.jersey.TransactionListener
import com.github.andrewoma.kwery.example.film.model.*
import com.github.andrewoma.kwery.example.film.resources.*
import com.github.andrewoma.kwery.fetcher.*

import com.github.andrewoma.kwery.mapper.Dao

import com.google.common.io.Resources
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment

class Daos(
        val actor: ActorDao,
        val language: LanguageDao,
        val film: FilmDao,
        val filmActor: FilmActorDao
)

class FilmApplication : Application<FilmConfiguration>() {
    override fun getName() = "film-app"

    override fun run(configuration: FilmConfiguration, environment: Environment) {
        val dataSource = configuration.database.build(environment.metrics(), "db")
        val session = ThreadLocalSession(dataSource, HsqlDialect(), LoggingInterceptor(infoQueryThresholdInMs = -1))

        environment.healthChecks().register("db", object : HealthCheck() {
            override fun check() = session.use {
                session.select("select 1 from information_schema.system_users") { }
                HealthCheck.Result.healthy()
            }
        })

        val daos = Daos(
                ActorDao(session),
                LanguageDao(session),
                FilmDao(session),
                FilmActorDao(session)
        )

        createAndLoadDb(environment, session, daos)

        val fetcher = createFetcher(daos)

        val jersey = environment.jersey()
        jersey.register(TransactionListener())
        jersey.register(FilmResource(daos.film, fetcher))
        jersey.register(ActorResource(daos.actor, fetcher))
        jersey.register(LanguageResource(daos.language, fetcher))
    }

    // Create an populate an in-memory database
    private fun createAndLoadDb(environment: Environment, session: ThreadLocalSession, daos: Daos) {
        createDb(session)
        load(environment, session, daos.actor, "actors.json")
        load(environment, session, daos.language, "languages.json")
        load(environment, session, daos.film, "films.json")
        load(environment, session, daos.filmActor, "film_actors.json")
    }

    inline fun <reified T> load(environment: Environment, session: ThreadLocalSession, dao: Dao<T, *>, resource: String) {
        environment.getObjectMapper().withObjectStream<T>(Resources.getResource(resource)) {
            for (values in it.chunked(50)) {
                session.use { dao.batchInsert(values) }
            }
        }
    }

    fun createDb(session: ThreadLocalSession) {
        for (sql in Resources.toString(Resources.getResource("schema.sql"), Charsets.UTF_8).split(";")) {
            session.use { session.update(sql) }
        }
    }

    override fun initialize(bootstrap: Bootstrap<FilmConfiguration>) {
        val mapper = bootstrap.getObjectMapper()
        mapper.registerModule(KotlinModule())
        mapper.registerModule(JSR310Module())

        mapper.addMixIn(javaClass<HasAttributeSet>(), javaClass<AttributeSetFilterMixIn>())
        val provider = SimpleFilterProvider().addFilter("Attribute set filter", AttributeSetFilter())
        mapper.setConfig(mapper.getSerializationConfig().withFilters(provider))

        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    fun createFetcher(daos: Daos): GraphFetcher {
        val language = Type(Language::id, { daos.language.findByIds(it) })
        val actor = Type(Actor::id, { daos.actor.findByIds(it) })

        val film = Type(Film::id, { daos.film.findByIds(it) }, listOf(
                Property(Film::language, language, { it.language.id }, {(f, l) -> f.copy(language = l) }),
                Property(Film::originalLanguage, language, { it.originalLanguage?.id }, {(f, l) -> f.copy(originalLanguage = l) }),
                CollectionProperty(Film::actors, actor, { it.id },
                        {(f, a) -> f.copy(actors = a.toSet()) },
                        { daos.actor.findByFilmIds(daos.filmActor.findByFilmIds(it)) })
        ))

        return GraphFetcher(setOf(language, actor, film))
    }
}