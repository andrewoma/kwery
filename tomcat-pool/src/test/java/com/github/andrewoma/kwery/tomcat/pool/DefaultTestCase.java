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
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.junit.After;
import org.junit.Before;


public abstract class DefaultTestCase {

    protected org.apache.tomcat.jdbc.pool.DataSource datasource;
    protected int threadcount = 10;

    public org.apache.tomcat.jdbc.pool.DataSource createDefaultDataSource() {
        PoolConfiguration p = new DefaultProperties();
        p.setFairQueue(false);
        p.setJmxEnabled(false);
        p.setTestWhileIdle(false);
        p.setTestOnBorrow(false);
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(threadcount);
        p.setInitialSize(threadcount);
        p.setMaxWait(10000);
        p.setRemoveAbandonedTimeout(10);
        p.setMinEvictableIdleTimeMillis(10000);
        p.setMinIdle(threadcount);
        p.setLogAbandoned(false);
        p.setRemoveAbandoned(false);

        org.apache.tomcat.jdbc.pool.DataSource datasource = new org.apache.tomcat.jdbc.pool.DataSource();
        datasource.setPoolProperties(p);
        return datasource;
    }

    @Before
    public void init() throws Exception {
        this.datasource = createDefaultDataSource();
    }

    @After
    public void tearDown() throws Exception {
        try {
            datasource.close();
        } catch (Exception ignore) {
            // Ignore
        }
        datasource = null;
        System.gc();
    }
}
