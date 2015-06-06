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

package com.github.andrewoma.kwery.core.interceptor

import com.github.andrewoma.kwery.core.interceptor.LoggingSummaryInterceptor.Execution
import kotlin.test.assertEquals
import org.junit.Test as test

class LoggingSummaryInterceptorTest {

    test fun `Summary report should be ranked by closed cost descending`() {
        val executions = arrayListOf(
                Execution(name = "ActorDao.findByIds", started = 42778208093556, executed = 42778209432595, closed = 42778209841378, rowCount = 200),
                Execution(name = "FilmActorDao.findByFilmIds", started = 42778185041832, executed = 42778200743160, closed = 42778206720613, rowCount = 5462),
                Execution(name = "FilmDao.findAll", started = 42778144102955, executed = 42778147628175, closed = 42778171385473, rowCount = 1000),
                Execution(name = "LanguageDao.findByIds", started = 42778179885222, executed = 42778181242601, closed = 42778181381269, rowCount = 1)
        )
        val requestTime = 203572525L
        val expected = """
Executed 4 statements in 21.923 ms (closed in 52.205 ms) affecting 6,663 rows using 25.6% of request total (203.573 ms):
                                Calls    Exec   Close   Rows      !
               FilmDao.findAll      1   3.525  27.283  1,000  52.3%
    FilmActorDao.findByFilmIds      1  15.701  21.679  5,462  41.5%
            ActorDao.findByIds      1   1.339   1.748    200   3.3%
         LanguageDao.findByIds      1   1.357   1.496      1   2.9%"""

        assertEquals(expected.replace("!".toRegex(), ""), generateReport(requestTime, executions))
    }

    test fun `Summary report should aggregate calls to same statement`() {
        val executions = arrayListOf(
                Execution(name = "ActorDao.findByIds", started = 42778208093556, executed = 42778209432595, closed = 42778209841378, rowCount = 200),
                Execution(name = "FilmActorDao.findByFilmIds", started = 42778185041832, executed = 42778200743160, closed = 42778206720613, rowCount = 5462),
                Execution(name = "FilmDao.findAll", started = 42778144102955, executed = 42778147628175, closed = 42778171385473, rowCount = 1000),
                Execution(name = "LanguageDao.findByIds", started = 42778179885222, executed = 42778181242601, closed = 42778181381269, rowCount = 1),
                Execution(name = "ActorDao.findByIds", started = 42778208093556, executed = 42778209432595, closed = 42778209841378, rowCount = 200),
                Execution(name = "FilmActorDao.findByFilmIds", started = 42778185041832, executed = 42778200743160, closed = 42778206720613, rowCount = 5462),
                Execution(name = "FilmDao.findAll", started = 42778144102955, executed = 42778147628175, closed = 42778171385473, rowCount = 1000),
                Execution(name = "LanguageDao.findByIds", started = 42778179885222, executed = 42778181242601, closed = 42778181381269, rowCount = 1)
        )
        val requestTime = 203572525L * 2
        val expected = """
Executed 8 statements in 43.846 ms (closed in 104.410 ms) affecting 19,989 rows using 25.6% of request total (407.145 ms):
                                Calls    Exec   Close    Rows      !
               FilmDao.findAll      2   7.050  54.565   2,000  52.3%
    FilmActorDao.findByFilmIds      2  31.403  43.358  10,924  41.5%
            ActorDao.findByIds      2   2.678   3.496     400   3.3%
         LanguageDao.findByIds      2   2.715   2.992       2   2.9%"""

        assertEquals(expected.replace("!".toRegex(), ""), generateReport(requestTime, executions))
    }

    fun generateReport(requestTime: Long, executions: MutableList<Execution>): String {
        val (totals, summaries) = LoggingSummaryInterceptor.summariseRequest(executions)
        return LoggingSummaryInterceptor.createReport(requestTime, totals, summaries)
    }
}