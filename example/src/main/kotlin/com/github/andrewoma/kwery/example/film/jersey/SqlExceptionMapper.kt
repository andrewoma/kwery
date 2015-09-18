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

package com.github.andrewoma.kwery.example.film.jersey

import com.github.andrewoma.kwery.mapper.OptimisticLockException
import io.dropwizard.jersey.errors.ErrorMessage
import io.dropwizard.jersey.errors.LoggingExceptionMapper
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
public class SqlExceptionMapper : LoggingExceptionMapper<SQLException>() {

    override fun toResponse(exception: SQLException): Response {
        val response = super.toResponse(exception) // Logs exception

        val code = when (exception) {
            is SQLIntegrityConstraintViolationException -> Response.Status.CONFLICT.statusCode
            is OptimisticLockException -> 428 // http://en.wikipedia.org/wiki/List_of_HTTP_status_codes#428
            else -> null
        }

        return if (code == null) response else Response.status(code)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ErrorMessage(code, exception.getMessage())).build()
    }
}