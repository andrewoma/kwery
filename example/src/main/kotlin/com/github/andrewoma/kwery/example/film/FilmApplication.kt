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

import io.dropwizard.Application
import io.dropwizard.setup.Environment
import io.dropwizard.setup.Bootstrap
import com.github.andrewoma.kwery.core.ThreadLocalSession
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.google.common.io.Resources
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.andrewoma.kwery.example.film.jersey.TransactionListener
import com.codahale.metrics.health.HealthCheck
import com.github.andrewoma.kwery.example.film.resources.FilmResource
import com.github.andrewoma.kwery.example.film.jackson.withObjectStream
import com.github.andrewoma.kommon.collection.chunked
import com.github.andrewoma.kwery.example.film.dao.ActorDao
import com.github.andrewoma.kwery.mapper.Dao
import com.github.andrewoma.kwery.example.film.resources.ActorResource
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.github.andrewoma.kwery.example.film.dao.LanguageDao
import com.github.andrewoma.kwery.example.film.dao.FilmDao
import com.github.andrewoma.kwery.example.film.model.HasAttributeSet
import com.github.andrewoma.kwery.example.film.jackson.AttributeSetFilterMixIn
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.github.andrewoma.kwery.example.film.jackson.AttributeSetFilter
import com.github.andrewoma.kwery.example.film.resources.LanguageResource

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

        val actorDao = ActorDao(session)
        val languageDao = LanguageDao(session)
        val filmDao = FilmDao(session)

        // Create an populate an in-memory database
        createDb(session)
        load(environment, session, actorDao, "actors.json")
        load(environment, session, languageDao, "languages.json")
        load(environment, session, filmDao, "films.json")

        val jersey = environment.jersey()
        jersey.register(TransactionListener())
        jersey.register(FilmResource(filmDao))
        jersey.register(ActorResource(actorDao))
        jersey.register(LanguageResource(languageDao))
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
}