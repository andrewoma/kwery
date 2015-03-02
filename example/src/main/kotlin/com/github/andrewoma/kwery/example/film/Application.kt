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

import io.dropwizard.Configuration
import io.dropwizard.Application
import io.dropwizard.setup.Environment
import io.dropwizard.setup.Bootstrap
import io.dropwizard.db.DataSourceFactory
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotNull
import javax.validation.Valid
import com.github.andrewoma.kwery.core.ThreadLocalSession
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import javax.sql.DataSource
import com.github.andrewoma.kwery.core.DefaultSession
import com.google.common.io.Resources

data class FilmConfiguration : Configuration() {
    // TODO ... devise a more elegant solution. Tried Jackson's Kotlin module but
    // DropWizard requires a default constructor
    Valid NotNull JsonProperty("database")
    private var _database: DataSourceFactory? = null
    val database: DataSourceFactory
        get() = _database!!
}

class FilmApplication : Application<FilmConfiguration>() {
    override fun getName() = "film-app"

    override fun run(configuration: FilmConfiguration, environment: Environment) {
        val dataSource = configuration.database.build(environment.metrics(), "db")
        val session = ThreadLocalSession(dataSource, HsqlDialect(), LoggingInterceptor())
        createDb(session)

        environment.jersey().register(FilmResource())
    }

    fun createDb(session: ThreadLocalSession) {
        session.use(startTransaction = false) {
            for (sql in Resources.toString(Resources.getResource("schema.sql"), Charsets.UTF_8).split(";")) {
                session.update(sql)
            }
        }
    }

    override fun initialize(bootstrap: Bootstrap<FilmConfiguration>) {
//        bootstrap.getObjectMapper().registerModule(KotlinModule())
    }
}

fun main(args: Array<String>) {
    val defaults = array("server", "example/src/main/resources/dev.yml")
    FilmApplication().run(*if (args.isEmpty()) defaults else args)
}
