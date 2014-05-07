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
package org.os890.ds.addon.async.event.impl;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.deltaspike.cdise.api.ContextControl;

import java.util.concurrent.Executor;

class RingBufferHolder<E>
{
    private final RingBuffer<DisruptorEventSlot<E>> ringBuffer;

    RingBufferHolder(Executor executor,
                     Integer bufferSize,
                     ProducerType producerType,
                     WaitStrategy waitStrategy,
                     ContextControl contextControl,
                     SlotEventHandler<DisruptorEventSlot<E>>... eventHandlers)
    {
        Disruptor<DisruptorEventSlot<E>> disruptor = new Disruptor<DisruptorEventSlot<E>>(new EventFactory<DisruptorEventSlot<E>>()
        {
            @Override
            public DisruptorEventSlot<E> newInstance()
            {
                return new DisruptorEventSlot<E>();
            }
        }, bufferSize, executor, producerType, waitStrategy);
        EventProcessor[] eventProcessors = new EventProcessor[eventHandlers.length];
        final SequenceBarrier barrier = disruptor.getRingBuffer().newBarrier();

        for (int i = 0; i < eventHandlers.length; i++)
        {
            SlotEventHandler<DisruptorEventSlot<E>> eventHandler = eventHandlers[i];
            eventProcessors[i] = createEventProcessor(disruptor, contextControl, eventHandler, barrier);
        }
        disruptor.handleEventsWith(eventProcessors);
        ringBuffer = disruptor.start();
    }

    private EventProcessor createEventProcessor(final Disruptor<DisruptorEventSlot<E>> disruptor,
                                                final ContextControl contextControl,
                                                final SlotEventHandler<DisruptorEventSlot<E>> eventHandler,
                                                final SequenceBarrier barrier)
    {
        return new CdiAwareEventProcessor(contextControl, disruptor.getRingBuffer(), eventHandler, barrier);
    }

    RingBuffer<DisruptorEventSlot<E>> getRingBuffer()
    {
        return ringBuffer;
    }
}
