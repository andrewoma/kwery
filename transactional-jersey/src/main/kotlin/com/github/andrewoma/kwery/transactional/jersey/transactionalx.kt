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
import java.lang.annotation.*
import java.lang.reflect.Method
import javax.ws.rs.ext.Provider

Target(ElementType.METHOD, ElementType.TYPE)
Retention(RetentionPolicy.RUNTIME)
Inherited
public annotation class transactionalx(
        /**
         * The name of the data source to use in the transaction
         */
        public val name: String = defaultThreadLocalSessionName,

        /**
         * If true, a session will be initialised but a transaction will not be started.
         * Transactions can be manually managed via the Session transaction functions.
         */
        public val manual: Boolean = false
)

Provider
public class TransactionListener : ApplicationEventListener {
    private val log = LoggerFactory.getLogger(javaClass<TransactionListener>())
    private var transactionals = hashMapOf<Method, transactionalx>()

    override fun onEvent(event: ApplicationEvent) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            registerResources(event.getResourceModel().getResources())
        }
    }

    private fun registerResources(resources: List<Resource>) {
        for (resource in resources) {
            val classAnnotation = resource.getAllMethods().firstOrNull()?.getInvocable()?.getDefinitionMethod()?.getDeclaringClass()?.getAnnotation(javaClass<transactionalx>())
            for (method in resource.getAllMethods()) {
                register(method, classAnnotation)
            }
            registerResources(resource.getChildResources())
        }
    }

    private fun register(method: ResourceMethod, transactional: transactionalx?) {
        val definitionMethod = method.getInvocable().getDefinitionMethod()
        val annotation = definitionMethod.getAnnotation(javaClass<transactionalx>()) ?: transactional
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
                val type = event.getType()
                if (type == RequestEvent.Type.REQUEST_MATCHED || type == RequestEvent.Type.FINISHED) {
                    val method = event.getUriInfo()?.getMatchedResourceMethod()?.getInvocable()?.getDefinitionMethod()
                    val transactional = transactionals[method]
                    if (transactional != null) {
                        if (type == RequestEvent.Type.REQUEST_MATCHED) {
                            ManagedThreadLocalSession.initialise(!transactional.manual, transactional.name)
                        } else {
                            ManagedThreadLocalSession.finalise(event.isSuccess())
                        }
                    }
                }
            } catch(e: Exception) {
                log.error("Error processing transaction listener", e)
            }
        }
    }
}