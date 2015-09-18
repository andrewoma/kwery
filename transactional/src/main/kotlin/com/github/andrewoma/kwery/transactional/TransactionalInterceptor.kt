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

package com.github.andrewoma.kwery.transactional

import com.github.andrewoma.kwery.core.ManagedThreadLocalSession
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import kotlin.reflect.KClass

class TransactionalInterceptor : MethodInterceptor {

    override fun invoke(invocation: MethodInvocation): Any? {
        val transactional = getTransactional(invocation)

        return if (transactional == null || ManagedThreadLocalSession.isInitialised(transactional.name)) {
            invocation.proceed()
        } else {
            invoke(transactional, invocation)
        }
    }

    private fun invoke(transactional: Transactional, invocation: MethodInvocation): Any? {
        var commit = true
        try {
            ManagedThreadLocalSession.initialise(!transactional.manual, transactional.name)
            return invocation.proceed()
        } catch(e: Exception) {
            commit = !rollbackOnException(transactional, e)
            throw e
        } finally {
            ManagedThreadLocalSession.finalise(commit, transactional.name)
        }
    }

    // Hacks to work around annotations KClass to Class conversions not working in M12
    // TODO - Remove this when conversions via filter functions don't throw ClassCastException
    private fun isInstance(exceptions: Array<KClass<out Exception>>, exception: Exception): Boolean {
        for (e in exceptions) {
            val clazz: Class<out Exception> = e.java
            if (clazz.isInstance(exception)) return true
        }
        return false
    }

    private fun rollbackOnException(transactional: Transactional, e: Exception): Boolean {
        return isInstance(transactional.rollbackOn, e) && !isInstance(transactional.ignore, e)
    }

    private fun getTransactional(invocation: MethodInvocation) =
            invocation.method.getAnnotation(Transactional::class.java)
                    ?: invocation.`this`.javaClass.getAnnotation(Transactional::class.java)
}

