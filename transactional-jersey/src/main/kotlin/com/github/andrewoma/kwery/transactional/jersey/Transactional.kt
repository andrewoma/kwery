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

package com.github.andrewoma.kwery.transactional.jersey

import com.github.andrewoma.kwery.core.ManagedThreadLocalSession
import com.github.andrewoma.kwery.core.defaultThreadLocalSessionName
import org.glassfish.jersey.server.model.Resource
import org.glassfish.jersey.server.model.ResourceMethod
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.slf4j.LoggerFactory
import java.lang.annotation.Inherited
import java.lang.reflect.Method
import javax.ws.rs.ext.Provider

@Inherited
annotation class Transactional(
        /**
         * The name of the data source to use in the transaction
         */
        val name: String = defaultThreadLocalSessionName,

        /**
         * If true, a session will be initialised but a transaction will not be started.
         * Transactions can be manually managed via the Session transaction functions.
         */
        val manual: Boolean = false
)

@Provider
class TransactionListener : ApplicationEventListener {
    private val log = LoggerFactory.getLogger(TransactionListener::class.java)
    private var transactionals = hashMapOf<Method, Transactional>()

    override fun onEvent(event: ApplicationEvent) {
        if (event.type == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            registerResources(event.resourceModel.resources)
        }
    }

    private fun registerResources(resources: List<Resource>) {
        for (resource in resources) {
            val classAnnotation = resource.allMethods.firstOrNull()?.invocable?.definitionMethod?.declaringClass?.getAnnotation(Transactional::class.java)
            for (method in resource.allMethods) {
                register(method, classAnnotation)
            }
            registerResources(resource.childResources)
        }
    }

    private fun register(method: ResourceMethod, transactional: Transactional?) {
        val definitionMethod = method.invocable.definitionMethod
        val annotation = definitionMethod.getAnnotation(Transactional::class.java) ?: transactional
        if (annotation != null) {
            transactionals[definitionMethod] = annotation
        }
    }

    override fun onRequest(event: RequestEvent): RequestEventListener {
        return Listener()
    }

    private inner class Listener : RequestEventListener {
        override fun onEvent(event: RequestEvent) {
            try {
                val type = event.type
                if (type == RequestEvent.Type.REQUEST_MATCHED || type == RequestEvent.Type.FINISHED) {
                    val method = event.uriInfo?.matchedResourceMethod?.invocable?.definitionMethod
                    if (method != null && method in transactionals) {
                        val transactional = transactionals[method]!!
                        if (type == RequestEvent.Type.REQUEST_MATCHED) {
                            ManagedThreadLocalSession.initialise(!transactional.manual, transactional.name)
                        } else {
                            ManagedThreadLocalSession.finalise(event.isSuccess)
                        }
                    }
                }
            } catch(e: Exception) {
                log.error("Error processing transaction listener", e)
            }
        }
    }
}