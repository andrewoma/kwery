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

package kwery

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class QuerySimulation extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:9090/api")
    .acceptHeader("application/json")

  val scn = scenario("Query api")
    .exec(http("Film/Id").get("/films/3?log=none").check(status.is(200)))
    .exec(http("Film/Id+Actors").get("/films/5?fetch=actors&log=none").check(status.is(200)))

  setUp(scn.inject(
    rampUsersPerSec(1) to 200 during (30 seconds),
    constantUsersPerSec(200) during (3 minutes),
    rampUsersPerSec(200) to 1 during (30 seconds)
  ).protocols(httpConf))
}
