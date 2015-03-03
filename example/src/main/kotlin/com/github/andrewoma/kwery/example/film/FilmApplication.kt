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

class FilmApplication : Application<FilmConfiguration>() {
    override fun getName() = "film-app"

    override fun run(configuration: FilmConfiguration, environment: Environment) {
        val dataSource = configuration.database.build(environment.metrics(), "db")
        val session = ThreadLocalSession(dataSource, HsqlDialect(), LoggingInterceptor(infoQueryThresholdInMs = -1))
        createDb(session)

        environment.healthChecks().register("db", object : HealthCheck() {
            override fun check() = session.use {
                session.select("select 1 from information_schema.system_users") { }
                HealthCheck.Result.healthy()
            }
        })

        val jersey = environment.jersey()
        jersey.register(TransactionListener())
        jersey.register(FilmResource(session))
    }

    fun createDb(session: ThreadLocalSession) {
        session.use(startTransaction = false) {
            for (sql in Resources.toString(Resources.getResource("schema.sql"), Charsets.UTF_8).split(";")) {
                session.update(sql)
            }
        }
    }

    override fun initialize(bootstrap: Bootstrap<FilmConfiguration>) {
        bootstrap.getObjectMapper().registerModule(KotlinModule())
    }
}