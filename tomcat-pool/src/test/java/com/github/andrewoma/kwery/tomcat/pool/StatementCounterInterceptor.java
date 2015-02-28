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

package com.github.andrewoma.kwery.tomcat.pool;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.apache.tomcat.jdbc.pool.interceptor.StatementDecoratorInterceptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor that counts opened Statements. Is used by tests.
 */
public class StatementCounterInterceptor extends StatementDecoratorInterceptor {

    private final AtomicInteger countOpen = new AtomicInteger();
    private final AtomicInteger countClosed = new AtomicInteger();

    public int getActiveCount() {
        return countOpen.get() - countClosed.get();
    }

    @Override
    protected Object createDecorator(Object proxy, Method method,
                                     Object[] args, Object statement, Constructor<?> constructor,
                                     String sql) throws InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Object result;
        StatementProxy statementProxy = new StatementProxy(
                (Statement) statement, sql);
        result = constructor.newInstance(new Object[]{statementProxy});
        statementProxy.setActualProxy(result);
        statementProxy.setConnection(proxy);
        statementProxy.setConstructor(constructor);
        countOpen.incrementAndGet();
        return result;
    }

    private class StatementProxy extends
            StatementDecoratorInterceptor.StatementProxy<Statement> {
        public StatementProxy(Statement delegate, String sql) {
            super(delegate, sql);
        }

        @Override
        public void closeInvoked() {
            countClosed.incrementAndGet();
            super.closeInvoked();
        }
    }
}
