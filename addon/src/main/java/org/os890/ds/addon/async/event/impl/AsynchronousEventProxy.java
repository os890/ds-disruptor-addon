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

import com.lmax.disruptor.RingBuffer;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.os890.ds.addon.async.event.api.AsynchronousEvent;
import org.os890.ds.addon.async.event.api.config.AsynchronousEventConfig;

import javax.enterprise.inject.spi.InjectionPoint;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

class AsynchronousEventProxy implements AsynchronousEvent, Serializable
{
    private final Class eventClass;
    private transient DisruptorEntry disruptorEntry;

    AsynchronousEventProxy(InjectionPoint injectionPoint,
                           DisruptorBroadcaster disruptorBroadcaster /*don't store it - it isn't a contextual reference*/)
    {
        if (injectionPoint.getMember() instanceof Field)
        {
            this.eventClass = (Class)((ParameterizedType)((Field)injectionPoint.getMember()).getGenericType()).getActualTypeArguments()[0];
        }
        else
        {
            throw new IllegalStateException("currently events are only supported via field-injection"); //TODO add more details about the usage
        }
        init(disruptorBroadcaster);
    }

    @Override
    public void fire(final Object event)
    {
        init(null);

        RingBuffer<DisruptorEventSlot> ringBuffer = disruptorEntry.getRingBuffer();
        long sequence = ringBuffer.next();
        try
        {
            DisruptorEventSlot slot = ringBuffer.get(sequence);
            slot.setEvent(event);
        }
        finally //try/finally approach required by disruptor
        {
            ringBuffer.publish(sequence);
        }
    }

    private void init(DisruptorBroadcaster disruptorBroadcaster)
    {
        if (this.disruptorEntry == null)
        {
            lazyInit(disruptorBroadcaster);
        }
    }

    private synchronized void lazyInit(DisruptorBroadcaster disruptorBroadcaster)
    {
        if (this.disruptorEntry != null)
        {
            return;
        }

        if (disruptorBroadcaster == null)
        {
            disruptorBroadcaster = BeanProvider.getContextualReference(DisruptorBroadcaster.class);
        }
        ConcurrentMap<Class, DisruptorEntry> disruptorEntries = disruptorBroadcaster.getDisruptorEntries();
        List<DisruptorObserverEntry> disruptorObserverEntries = disruptorBroadcaster.getDisruptorExtension().getDisruptorObserver(this.eventClass);
        ContextControl contextControl = disruptorBroadcaster.getContextControl();
        this.disruptorEntry = disruptorEntries.get(this.eventClass);

        if(disruptorEntry == null)
        {
            AsynchronousEventConfig config = BeanProvider.getContextualReference(AsynchronousEventConfig.class);

            SlotEventHandler<DisruptorEventSlot>[] slotEventHandler = new SlotEventHandler[disruptorObserverEntries.size()];

            for (int i = 0; i < disruptorObserverEntries.size(); i++)
            {
                slotEventHandler[i] = new SlotEventHandler(disruptorObserverEntries.get(i));
            }

            disruptorEntry = new DisruptorEntry(
                    config.getExecutor(),
                    config.getRingBufferSize(),
                    config.getProducerType(),
                    config.getWaitStrategy(),
                    contextControl, slotEventHandler);
            //TODO init
            DisruptorEntry existingDisruptorEntry = disruptorEntries.putIfAbsent(this.eventClass, disruptorEntry);
            if (existingDisruptorEntry != null)
            {
                disruptorEntry = existingDisruptorEntry;
            }
        }
    }
}
