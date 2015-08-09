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
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.github.andrewoma.kommon.collection.chunked
import com.github.andrewoma.kwery.core.DefaultSession
import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.ThreadLocalSession
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.github.andrewoma.kwery.core.interceptor.LoggingSummaryInterceptor
import com.github.andrewoma.kwery.core.interceptor.StatementInterceptorChain
import com.github.andrewoma.kwery.example.film.dao.*
import com.github.andrewoma.kwery.example.film.jackson.AttributeSetFilter
import com.github.andrewoma.kwery.example.film.jackson.AttributeSetFilterMixIn
import com.github.andrewoma.kwery.example.film.jackson.withObjectStream
import com.github.andrewoma.kwery.example.film.jersey.LoggingListener
import com.github.andrewoma.kwery.example.film.jersey.SqlExceptionMapper
import com.github.andrewoma.kwery.example.film.model.Actor
import com.github.andrewoma.kwery.example.film.model.Film
import com.github.andrewoma.kwery.example.film.model.HasAttributeSet
import com.github.andrewoma.kwery.example.film.model.Language
import com.github.andrewoma.kwery.example.film.resources.ActorResource
import com.github.andrewoma.kwery.example.film.resources.FilmResource
import com.github.andrewoma.kwery.example.film.resources.LanguageResource
import com.github.andrewoma.kwery.fetcher.CollectionProperty
import com.github.andrewoma.kwery.fetcher.GraphFetcher
import com.github.andrewoma.kwery.fetcher.Property
import com.github.andrewoma.kwery.fetcher.Type
import com.github.andrewoma.kwery.mapper.Dao
import com.github.andrewoma.kwery.mapper.listener.*
import com.github.andrewoma.kwery.transactional.jersey.TransactionListener
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.io.Resources
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class FilmApplication : Application<FilmConfiguration>() {
    val log = LoggerFactory.getLogger(javaClass)

    override fun getName() = "film-app"

    override fun run(configuration: FilmConfiguration, environment: Environment) {
        DefaultSession.namedQueryCache = GuavaCache()

        val dataSource = configuration.database.build(environment.metrics(), "db")
        val interceptors = StatementInterceptorChain(listOf(
                LoggingInterceptor(infoQueryThresholdInMs = 1000),
                LoggingSummaryInterceptor()))

        val session = ThreadLocalSession(dataSource, HsqlDialect(), interceptors)

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

        val caches = createCaches(daos)

        val fetcher = createFetcher(daos, caches)

        val jersey = environment.jersey()
        environment.jersey().setUrlPattern("/api/*");
        jersey.register(SqlExceptionMapper())
        jersey.register(LoggingListener())
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
        for (sql in Resources.toString(Resources.getResource("schema.sql"), Charsets.UTF_8).split(";".toRegex())) {
            session.use { session.update(sql) }
        }
    }

    override fun initialize(bootstrap: Bootstrap<FilmConfiguration>) {
        bootstrap.addBundle(AssetsBundle("/static", "/", "index.html", "static"))

        val mapper = bootstrap.getObjectMapper()
        mapper.registerModule(JSR310Module())

        mapper.addMixIn(javaClass<HasAttributeSet>(), javaClass<AttributeSetFilterMixIn>())
        val provider = SimpleFilterProvider().addFilter("Attribute set filter", AttributeSetFilter())
        mapper.setConfig(mapper.getSerializationConfig().withFilters(provider))

        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    fun createCaches(daos: Daos): Caches {
        // Create a Guava-backed cache that loads from the dao on demand
        // For clusters this could be a distributed cache. Alternatively, the invalidation events below
        // could be broadcast to the cluster
        val caches = Caches(CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(object : CacheLoader<Int, Language>() {
                    override fun load(key: Int) = daos.language.findById(key)
                    override fun loadAll(keys: Iterable<Int>) = daos.language.findByIds(keys.toList())
                }))


        // Add a listener to invalidate on update
        daos.language.addListener(object : DeferredListener() {
            override fun onCommit(committed: Boolean, events: List<Event>) {
                if (committed) {
                    for (event in events) handleEvent(event)
                }
            }

            fun handleEvent(event: Event) {
                when (event) {
                    is UpdateEvent, is DeleteEvent -> {
                        log.info("Invalidating language cache for id ${event.id}")
                        caches.language.invalidate(event.id)
                    }
                }
            }
        })

        return caches
    }

    fun createFetcher(daos: Daos, caches: Caches): GraphFetcher {
        val language = Type(Language::id, {
            log.info("Requesting objects from Language cache with ids: $it")
            caches.language.getAll(it)
        })

        val actor = Type(Actor::id, { daos.actor.findByIds(it) })

        val film = Type(Film::id, { daos.film.findByIds(it) }, listOf(
                Property(Film::language, language, { it.language.id }, { f, l -> f.copy(language = l) }),
                Property(Film::originalLanguage, language, { it.originalLanguage?.id }, { f, l -> f.copy(originalLanguage = l) }),
                CollectionProperty(Film::actors, actor, { it.id },
                        { f, a -> f.copy(actors = a.toSet()) },
                        { daos.actor.findByFilmIds(daos.filmActor.findByFilmIds(it)) })
        ))

        // The model defines a cycle, so set the actor properties after film is defined
        // While models support cycles, fetching cycles isn't recommended
        actor.properties = listOf(CollectionProperty(Actor::films, film, { it.id },
                { a, f -> a.copy(films = f.toSet()) },
                { daos.film.findByActorIds(daos.filmActor.findByActorIds(it)) }))

        return GraphFetcher(setOf(language, actor, film))
    }
}

class Daos(
        val actor: ActorDao,
        val language: LanguageDao,
        val film: FilmDao,
        val filmActor: FilmActorDao
)

class Caches(val language: LoadingCache<Int, Language>)
