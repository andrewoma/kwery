/*
 * Copyright (c) 2016 Andrew O'Malley
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

package com.github.andrewoma.kwery.core.builder

import com.github.andrewoma.kwery.core.AbstractFilmSessionTest
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class QueryBuilderTest : AbstractFilmSessionTest() {

    @Test fun `should support select`() {
        assertSql(setOf(), """
           select * from actor
        """)
        {
            select("select * from actor")
        }
    }

    @Test fun `should support select with ordering`() {
        assertSql(setOf(), """
           select * from actor
           order by actor_id
        """)
        {
            select("select * from actor")
            orderBy("actor_id")
        }
    }

    @Test fun `should support where clause`() {
        assertSql(setOf(), """
           select * from actor
           where first_name = 'Kate'
        """)
        {
            select("select * from actor")
            whereGroup {
                where("first_name = 'Kate'")
            }
        }
    }

    @Test fun `should support where clause with param`() {
        assertSql(setOf("first_name"), """
           select * from actor
           where first_name = :first_name
        """)
        {
            select("select * from actor")
            whereGroup {
                where("first_name = :first_name")
                parameter("first_name", "Kate")
            }
        }
    }

    @Test fun `should support where clause with params`() {
        assertSql(setOf("first_name", "last_name"), """
           select * from actor
           where first_name = :first_name and last_name = :last_name
        """)
        {
            select("select * from actor")
            whereGroup {
                where("first_name = :first_name")
                parameter("first_name", "Kate")

                where("last_name = :last_name")
                parameter("last_name", "Beckinsale")
            }
        }
    }

    @Test fun `should support where clause with params and operator`() {
        assertSql(setOf("first_name", "last_name"), """
           select * from actor
           where first_name = :first_name or last_name = :last_name
        """)
        {
            select("select * from actor")
            whereGroup("or") {
                where("first_name = :first_name")
                parameter("first_name", "Kate")

                where("last_name = :last_name")
                parameter("last_name", "Beckinsale")
            }
        }
    }

    @Test fun `should support nested where clause`() {
        assertSql(setOf(), """
           select * from actor
           where last_name = 'Beckinsale' and
           (first_name = 'Kate' or first_name = 'Cate')
        """)
        {
            select("select * from actor")
            whereGroup {
                where("last_name = 'Beckinsale'")
                whereGroup("or") {
                    where("first_name = 'Kate'")
                    where("first_name = 'Cate'")
                }
            }
        }
    }

    @Test fun `should collapse empty where groups`() {
        assertSql(setOf(), """
           select * from actor
        """)
        {
            select("select * from actor")
            whereGroup {
                whereGroup("or") {
                }
            }
        }
    }

    @Test fun `should support groupBy clause`() {
        assertSql(setOf(), """
           select actor_id, count(*) from actor
           group by actor_id
        """)
        {
            select("select actor_id, count(*) from actor")
            groupBy("actor_id")
        }
    }

    @Test fun `should support groupBy with having clause`() {
        assertSql(setOf(), """
           select actor_id, count(*) from actor
           group by actor_id
           having count(*) > 1
        """)
        {
            select("select actor_id, count(*) from actor")
            groupBy("actor_id")
            having("count(*) > 1")
        }
    }

    // TODO ... seems valid but HSQLDB doesn't allow it
    @Ignore @Test fun `should support all options`() {
        assertSql(setOf("id_1", "id_2", "id_3"), """
            select a.actor_id, count(*) from actor a
            inner join actor a2 on a2.actor_id = a.actor_id
            where a.actor_id = :id_1 or
            (a.actor_id = :id_2 or a.actor_id = :id_3)
            group by actor_id
            having count(*) > 1
            order by a.actor_id
        """)
        {
            select("select a.actor_id, count(*) from actor a")
            select("inner join actor a2 on a2.actor_id = a.actor_id")
            whereGroup("or") {
                where("a.actor_id = :id_1")
                parameter("id_1", 1)
                whereGroup("or") {
                    where("a.actor_id = :id_2")
                    parameter("id_2", 2)
                    where("a.actor_id = :id_3")
                    parameter("id_3", 3)
                }
            }
            groupBy("actor_id")
            having("count(*) > 1")
            orderBy("a.actor_id")
        }
    }

    @Test fun `should support multiple select clauses`() {
        assertSql(setOf(), """
           select a.* from actor a
           inner join actor a2 on a2.actor_id = a.actor_id
        """)
        {
            select("select a.* from actor a")
            select("inner join actor a2 on a2.actor_id = a.actor_id")
        }
    }

    fun assertSql(expectedParams: Set<String>, expectedSql: String, block: QueryBuilder.() -> Unit) {
        val query = QueryBuilder().build(block = block)
        assertEquals(expectedParams, query.parameters.keys)
        assertEquals(expectedSql.trimIndent(), query.sql.replace(" $".toRegex(RegexOption.MULTILINE), ""))
        verifySql(query)
    }

    fun verifySql(query: Query) {
        // Results don't matter, just verify the syntax is valid
        session.select(query.sql, query.parameters) {
        }
    }
}