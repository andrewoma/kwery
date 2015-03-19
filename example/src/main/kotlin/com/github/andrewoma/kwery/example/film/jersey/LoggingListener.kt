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

import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import com.github.andrewoma.kwery.core.interceptor.LoggingSummaryInterceptor
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.slf4j.LoggerFactory
import javax.ws.rs.ext.Provider


Provider
public class LoggingListener : ApplicationEventListener {
    private val log = LoggerFactory.getLogger(javaClass<LoggingListener>())

    enum class LogType { none summary statements all }

    override fun onEvent(event: ApplicationEvent) {
    }

    override fun onRequest(requestEvent: RequestEvent): RequestEventListener {
        return Listener()
    }

    private inner class Listener : RequestEventListener {
        var logType = LogType.summary

        override fun onEvent(event: RequestEvent) {
            try {
                val type = event.getType()

                if (type == RequestEvent.Type.MATCHING_START) {
                    logType = getLogType(event)
                    if (logType == LogType.summary || logType == LogType.all) {
                        LoggingSummaryInterceptor.start()
                    }
                    if (logType == LogType.statements || logType == LogType.all) {
                        LoggingInterceptor.forceLogging.set(true)
                    }
                } else if (type == RequestEvent.Type.FINISHED) {
                    if (logType == LogType.summary || logType == LogType.all) {
                        LoggingSummaryInterceptor.stop()
                    }
                    if (logType == LogType.statements || logType == LogType.all) {
                        LoggingInterceptor.forceLogging.remove()
                    }
                }
            } catch(e: Exception) {
                log.error("Error processing log listener", e)
            }
        }

        private fun getLogType(event: RequestEvent): LogType {
            val logParam = event.getContainerRequest().getUriInfo().getQueryParameters().getFirst("log")
            return if (logParam == null) LogType.summary else {
                try {
                    LogType.valueOf(logParam)
                } catch(e: Exception) {
                    log.warn("Invalid logging value: $logParam")
                    LogType.summary
                }
            }
        }
    }
}
