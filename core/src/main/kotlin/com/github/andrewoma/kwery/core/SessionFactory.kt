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

package com.github.andrewoma.kwery.core

import java.sql.Connection
import com.github.andrewoma.kwery.core.dialect.Dialect
import com.github.andrewoma.kwery.core.interceptor.StatementInterceptor
import com.github.andrewoma.kwery.core.interceptor.noOpStatementInterceptor
import javax.sql.DataSource

public class SessionFactory(val dataSource: DataSource,
                            val dialect: Dialect,
                            val interceptor: StatementInterceptor = noOpStatementInterceptor,
                            val defaultSelectOptions: SelectOptions = SelectOptions(),
                            val defaultUpdateOptions: UpdateOptions = UpdateOptions()
) {

    public fun <R> use(f: (Session) -> R): R {
        val connection = dataSource.getConnection()
        try {
            val session = DefaultSession(connection, dialect, interceptor, defaultSelectOptions, defaultUpdateOptions)
            return f(session)
        } finally {
            connection.close()
        }
    }
}