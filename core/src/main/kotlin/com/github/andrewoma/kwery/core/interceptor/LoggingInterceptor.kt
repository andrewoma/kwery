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

import com.github.andrewoma.kommon.util.StopWatch
import com.github.andrewoma.kwery.core.ExecutingStatement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * LoggingInterceptor logs full SQL statements to the logger provided with parameters bound inline.
 *
 * The amount it logs depends on the logger's level:
 * * Debug: Everything
 * *  Info: Anything slower than 'infoQueryThresholdInMs'
 * *  Warn: Anything statements that return SQLWarnings (TODO)
 * * Error: Anything that throws an exception
 *
 *  Logging can also be forced on a per thread basis via `forceLogging`.
 *  This allows a particular request to logged without turning on all logging for a server.
 *  e.g. Add "sqlLogging=true" to an http request that sets forceLogging=true in a servlet filter
 */
// TODO - Add support for SQL Warnings
open public class LoggingInterceptor(val log: Logger = LoggerFactory.getLogger(javaClass<LoggingInterceptor>()),
                                     val infoQueryThresholdInMs: Long = 1000L,
                                     val parameterLimit: Int = -1
) : StatementInterceptor {
    companion object {
        public val forceLogging: ThreadLocal<Boolean> = ThreadLocal()
    }

    data class Context(val stopWatch: StopWatch, val executedTiming: String = "", val exception: Exception? = null)

    var ExecutingStatement.context: Context
        get() = this.contexts[javaClass<LoggingInterceptor>().getName()] as Context
        set(value) {
            this.contexts[javaClass<LoggingInterceptor>().getName()] = value
        }

    override fun executed(statement: ExecutingStatement) {
        statement.context = statement.context.copy(executedTiming = statement.context.stopWatch.toString())
    }

    override fun construct(statement: ExecutingStatement): ExecutingStatement {
        statement.context = Context(StopWatch().start())
        return statement
    }

    override fun closed(statement: ExecutingStatement) {
        val context = statement.context
        when {
            forceLogging.get() ?: false -> log.info(createMessage(statement))
            log.isErrorEnabled() && context.exception != null -> log.error(createMessage(statement))
            log.isInfoEnabled() && context.stopWatch.elapsed(MILLISECONDS) >= infoQueryThresholdInMs -> log.info(createMessage(statement))
            log.isDebugEnabled() -> log.debug(createMessage(statement))
        }
    }

    private fun createMessage(statement: ExecutingStatement): String {
        val context = statement.context
        val closedTiming = context.stopWatch.toString()
        val sb = StringBuilder()

        for (parameters in statement.parametersList) {
            sb.append("\n" + statement.session.bindParameters(statement.sql, parameters, false, parameterLimit, false)).append(";")
        }
        val batch = statement.parametersList.size().let { if (it > 1) "for batch of $it " else "" }

        val count = statement.rowsCounts
        val rowCount = when (count.size()) {
            0 -> ""
            1 -> ". Rows affected: ${count.first()}"
            else -> ". Rows affected: ${count.reduce { sum, i -> sum + i }} (${count.joinToString(", ")})"
        }

        val timing = "${statement.options.name ?: "statement"} ${batch}in ${context.executedTiming} ($closedTiming)${rowCount}"
        val message = if (context.exception == null) {
            "\nSucessfully executed $timing"
        } else {
            "\nFailed to execute $timing\nReason: ${context.exception.getMessage()}"
        }

        sb.append(message)
        sb.append(". TXN: ")
        sb.append(statement.session.currentTransaction?.id ?: "None")
        sb.append(additionalInfo(statement))
        sb.append("\n")
        return sb.toString()
    }

    protected open fun additionalInfo(statement: ExecutingStatement): String = ""

    override fun exception(statement: ExecutingStatement, e: Exception): Exception {
        statement.context = statement.context.copy(exception = e)
        return e
    }
}
