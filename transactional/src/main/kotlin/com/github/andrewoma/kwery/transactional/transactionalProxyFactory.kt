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

import com.github.andrewoma.kwery.core.Session
import com.github.andrewoma.kwery.core.ThreadLocalSession
import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import kotlin.reflect.KMemberProperty
import kotlin.reflect.jvm.javaField

object transactionalProxyFactory {
    fun <T> fromInterfaces(obj: T, interfaces: Array<Class<*>> = obj.javaClass.getInterfaces()): T {
        val enhancer = Enhancer()
        enhancer.setInterfaces(interfaces)
        enhancer.setCallback(MethodInterceptor() { dontuse, method, args, proxy ->
            TransactionalInterceptor().invoke(object : MethodInvocation {
                override fun getThis() = obj
                override fun getStaticPart() = method
                override fun proceed() = proxy.invoke(obj, args)
                override fun getMethod() = method
                override fun getArguments() = args
            })
        })

        @suppress("UNCHECKED_CAST")
        return enhancer.create() as T
    }

    fun <T> fromClass(obj: T, args: List<KMemberProperty<T, *>>): T {
        return fromClass(obj, args.map { it.javaField!!.getType() }.toTypedArray(), args.map { it.get(obj) }.toTypedArray())
    }

    fun <T> fromClass(obj: T, argTypes: Array<Class<*>>, args: Array<Any?>): T {
        val enhancer = Enhancer()
        enhancer.setSuperclass(obj.javaClass)
        enhancer.setCallback(MethodInterceptor() { dontuse, method, args, proxy ->
            TransactionalInterceptor().invoke(object : MethodInvocation {
                override fun getThis() = obj
                override fun getStaticPart() = method
                override fun proceed() = proxy.invoke(obj, args)
                override fun getMethod() = method
                override fun getArguments() = args
            })
        })

        @suppress("UNCHECKED_CAST")
        return enhancer.create(argTypes, args) as T
    }
}