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

import org.glassfish.jersey.server.model.ResourceMethod
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener

import javax.ws.rs.ext.Provider
import java.lang.reflect.Method

import com.github.andrewoma.kwery.core.ThreadLocalSession
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Retention
import org.slf4j.LoggerFactory

Retention(RetentionPolicy.RUNTIME)
public annotation class Transaction

Provider
public class TransactionListener : ApplicationEventListener {
    private val log = LoggerFactory.getLogger(javaClass<TransactionListener>())
    private var transactions = hashSetOf<Method>()

    override fun onEvent(event: ApplicationEvent) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {

            for (resource in event.getResourceModel().getResources()) {
                for (method in resource.getAllMethods()) {
                    register(method)
                }

                for (childResource in resource.getChildResources()) {
                    for (method in childResource.getAllMethods()) {
                        register(method)
                    }
                }
            }
        }
    }

    private fun register(method: ResourceMethod) {
        val definitionMethod = method.getInvocable().getDefinitionMethod()
        val annotation = definitionMethod.getAnnotation(javaClass<Transaction>())
        if (annotation != null) {
            transactions.add(definitionMethod)
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
                    if (method in transactions) {
                        if (type == RequestEvent.Type.REQUEST_MATCHED) {
                            ThreadLocalSession.initialise(true)
                        } else {
                            ThreadLocalSession.finalise(event.isSuccess())
                        }
                    }
                }
            } catch(e: Exception) {
                log.error("Error processing transaction listener", e)
            }
        }
    }
}