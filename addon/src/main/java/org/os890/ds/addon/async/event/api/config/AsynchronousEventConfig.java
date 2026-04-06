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

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Default configuration for the Disruptor-based asynchronous event system.
 * Override with {@code @Specializes} to provide custom settings for ring buffer size,
 * executor, producer type, or wait strategy.
 */
@ApplicationScoped
public class AsynchronousEventConfig
{
    private Executor executor = Executors.newCachedThreadPool();

    /**
     * Returns the size of the Disruptor ring buffer. Must be a power of two.
     *
     * @return the ring buffer size (default 4096)
     */
    public int getRingBufferSize()
    {
        return 4096;
    }

    /**
     * Returns the executor used to run event processors.
     *
     * @return the executor instance (default cached thread pool)
     */
    public Executor getExecutor()
    {
        return executor;
    }

    /**
     * Returns the Disruptor producer type.
     *
     * @return the producer type (default {@link ProducerType#MULTI})
     */
    public ProducerType getProducerType()
    {
        return ProducerType.MULTI;
    }

    /**
     * Returns the wait strategy used by the Disruptor.
     *
     * @return the wait strategy (default {@link BlockingWaitStrategy})
     */
    public WaitStrategy getWaitStrategy()
    {
        return new BlockingWaitStrategy();
    }
}
