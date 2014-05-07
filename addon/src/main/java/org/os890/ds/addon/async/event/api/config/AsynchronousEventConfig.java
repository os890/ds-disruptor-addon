/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.os890.ds.addon.async.event.api.config;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//use e.g. @Specializes to provide a custom config
@ApplicationScoped
public class AsynchronousEventConfig
{
    private Executor executor = Executors.newCachedThreadPool();

    public int getRingBufferSize()
    {
        return 4096;
    }

    public Executor getExecutor()
    {
        return executor;
    }

    public ProducerType getProducerType()
    {
        return ProducerType.MULTI;
    }

    public WaitStrategy getWaitStrategy()
    {
        return new BlockingWaitStrategy();
    }
}
