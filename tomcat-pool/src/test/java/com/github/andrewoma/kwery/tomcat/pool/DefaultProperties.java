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

import org.apache.tomcat.jdbc.pool.DataSourceFactory;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.util.Properties;

/**
 * @version 1.0
 */
public class DefaultProperties extends PoolProperties {
    private static final long serialVersionUID = 1L;

    public DefaultProperties() {
        setDbProperties(new Properties());

        setUrl(System.getProperty("url", "jdbc:h2:~/.h2/test;QUERY_TIMEOUT=0;DB_CLOSE_ON_EXIT=FALSE"));
        setDriverClassName(System.getProperty("driverClassName", "org.h2.Driver"));
        System.setProperty("h2.serverCachedObjects", "10000");

        setPassword(System.getProperty("password", "password"));
        setUsername(System.getProperty("username", "root"));

        setValidationQuery(System.getProperty("validationQuery", "SELECT 1"));
        setDefaultAutoCommit(Boolean.TRUE);
        setDefaultReadOnly(Boolean.FALSE);
        setDefaultTransactionIsolation(DataSourceFactory.UNKNOWN_TRANSACTIONISOLATION);
        setConnectionProperties(null);
        setDefaultCatalog(null);
        setInitialSize(10);
        setMaxActive(100);
        setMaxIdle(getInitialSize());
        setMinIdle(getInitialSize());
        setMaxWait(10000);

        setTestOnBorrow(true);
        setTestOnReturn(false);
        setTestWhileIdle(true);
        setTimeBetweenEvictionRunsMillis(5000);
        setNumTestsPerEvictionRun(0);
        setMinEvictableIdleTimeMillis(1000);
        setRemoveAbandoned(true);
        setRemoveAbandonedTimeout(5000);
        setLogAbandoned(true);
        setValidationInterval(0); //always validate
        setInitSQL(null);
        setTestOnConnect(false);
        getDbProperties().setProperty("user", getUsername());
        getDbProperties().setProperty("password", getPassword());
    }
}
