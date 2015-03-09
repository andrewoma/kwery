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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.github.andrewoma.kommon.util.StopWatch
import com.github.andrewoma.kwery.core.ExecutingStatement
import java.util.Collections
import java.util.ArrayList
import java.util.concurrent.TimeUnit

public class LoggingSummaryInterceptor : StatementInterceptor {

    class Request(val stopWatch: StopWatch, val executions: MutableList<Execution>)

    class object {
        val requests: ThreadLocal<Request> = ThreadLocal()
        val nanoToMs = 1000000

        public fun start() {
            check(requests.get() == null, "LoggingSummaryInterceptor already started")
            requests.set(Request(StopWatch().start(), arrayListOf()))
        }

        public fun stop(log: Logger = LoggerFactory.getLogger(javaClass<LoggingSummaryInterceptor>())) {
            val request = requests.get()
            if (request == null) {
                log.warn("LoggingSummaryInterceptor not started")
                return
            }
            requests.remove()
            request.stopWatch.stop()

            if (request.executions.isEmpty()) return

            val (totals, summaries) = summariseRequest(request.executions)
            log.info(createReport(request.stopWatch.elapsed(TimeUnit.NANOSECONDS), totals, summaries))
        }

        fun calculateWidths(totals: ExecutionSummary, summaries: List<ExecutionSummary>): List<Int> {
            var name = 0
            var executions = 0
            var closed = 0
            var executed = 0
            var rowCount = 0
            var percentage = 0

            for (summary in summaries) {
                name = Math.max(name, summary.name.length())
                executions = Math.max(executions, width(summary.executionCount))
                closed = Math.max(closed, width(summary.closedTime / nanoToMs))
                executed = Math.max(executed, width(summary.executionTime / nanoToMs))
                rowCount = Math.max(rowCount, width(summary.rowCounts))
                percentage = Math.max(percentage, width((summary.executionTime.toDouble() / totals.executionTime.toDouble() * 100).toLong()))
            }

            return listOf(name, executions, executed + 4, closed + 4, rowCount, percentage + 2)
        }

        fun width(count: Long): Int {
            val digits = if (count == 0L) 1 else Math.log10(count.toDouble()).toInt() + 1
            return digits + ((digits - 1) / 3)
        }

        fun createReport(requestTime: Long, totals: ExecutionSummary, summaries: List<ExecutionSummary>): String {
            val sb = StringBuilder()
            sb.append("\nExecuted %,d statements in %,.3f ms (closed in %,.3f ms) affecting %,d rows using %.1f%% of request total (%,.3f ms):\n".format(
                    totals.executionCount,
                    totals.executionTime.toDouble() / nanoToMs,
                    totals.closedTime.toDouble() / nanoToMs,
                    totals.rowCounts,
                    totals.closedTime.toDouble() / requestTime.toDouble() * 100,
                    requestTime.toDouble() / nanoToMs
            ))
            createReport(sb, totals, summaries)
            return sb.toString()
        }

        fun createReport(sb: StringBuilder, totals: ExecutionSummary, summaries: List<ExecutionSummary>) {
            val headings = listOf("", "Calls", "Exec", "Close", "Rows", "")
            val widths = calculateWidths(totals, summaries).zip(headings).map { Math.max(it.first, it.second.length()) }

            val format = "    %${widths[0]}s  %,${widths[1]}d  %,${widths[2]}.${3}f  %,${widths[3]}.${3}f  %,${widths[4]}d  %${widths[5]}.${1}f%%"
            val headingsFormat = "    %${widths[0]}s  %${widths[1]}s  %${widths[2]}s  %${widths[3]}s  %${widths[4]}s  %${widths[5]}s"

            sb.append(headingsFormat.format(*headings.copyToArray()))
            for (summary in summaries) {
                sb.append("\n").append(printSummary(format, totals, summary))
            }
        }

        fun printSummary(format: String, totals: ExecutionSummary, summary: ExecutionSummary): String {
            return format.format(summary.name,
                    summary.executionCount,
                    summary.executionTime.toDouble() / nanoToMs,
                    summary.closedTime.toDouble() / nanoToMs,
                    summary.rowCounts,
                    summary.closedTime.toDouble() / totals.closedTime.toDouble() * 100)
        }

        fun summariseRequest(executions: MutableList<Execution>): Pair<ExecutionSummary, List<ExecutionSummary>> {
            // Prematurely optimise with some imperative code to summarise without lots of collection creation
            // Group by statement name (and collect totals)
            Collections.sort(executions, {(e1, e2) -> e1.name.compareTo(e2.name) })
            val summaries = ArrayList<ExecutionSummary>(executions.size() + 1)

            var total = ExecutionSummary("Total", 0L, 0L, 0L, 0L)
            var current = total

            for (execution in executions) {
                if (current.name != execution.name) {
                    current = ExecutionSummary(execution.name, 0L, 0L, 0L, 0L)
                    summaries.add(current)
                }
                current.closedTime += execution.closed - execution.started
                current.executionTime += execution.executed - execution.started
                current.rowCounts += execution.rowCount
                current.executionCount++

                total.closedTime += execution.closed - execution.started
                total.executionTime += execution.executed - execution.started
                total.rowCounts += current.rowCounts
                total.executionCount++
            }

            // Sort by closed time descending (usually what you want unless streaming)
            Collections.sort(summaries, {(s1, s2) -> s1.closedTime.compareTo(s2.closedTime) * -1 })

            return total to summaries
        }
    }

    data class ExecutionSummary(val name: String, var executionTime: Long, var closedTime: Long, var executionCount: Long, var rowCounts: Long)
    data class Execution(val name: String, val started: Long, val executed: Long, val closed: Long, val rowCount: Long)

    var ExecutingStatement.context: Execution?
        get() = this.contexts[javaClass<LoggingSummaryInterceptor>().getName()] as Execution?
        set(value) {
            this.contexts[javaClass<LoggingSummaryInterceptor>().getName()] = value
        }

    override fun executed(statement: ExecutingStatement) {
        if (statement.context != null) {
            statement.context = statement.context?.copy(executed = System.nanoTime())
        }
    }

    override fun construct(statement: ExecutingStatement): ExecutingStatement {
        if (requests.get() == null) return statement

        val currentTime = System.nanoTime()
        statement.context = Execution(statement.options.name ?: "Unknown ${statement.sql.hashCode()}",
                currentTime, currentTime, currentTime, 0)

        return statement
    }

    override fun closed(statement: ExecutingStatement) {
        if (statement.context != null) {
            val rowCount = statement.rowsCounts.fold(0L, {(acc, i) -> acc + i })
            requests.get()?.executions?.add(statement.context?.copy(closed = System.nanoTime(), rowCount = rowCount)!!)
        }
    }
}
