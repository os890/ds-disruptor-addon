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
import org.apache.deltaspike.core.api.literal.DefaultLiteral;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.os890.ds.addon.async.event.api.AsynchronousEvent;
import org.os890.ds.addon.async.event.api.config.AsynchronousEventConfig;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

class AsynchronousEventProxy implements AsynchronousEvent, Serializable
{
    private static final long serialVersionUID = -3759644415170905068L;

    private final int eventClassAndQualifierHashCode;
    private transient RingBufferHolder ringBufferHolder;

    AsynchronousEventProxy(BeanManager beanManager,
                           InjectionPoint injectionPoint,
                           AsynchronousEventProducer asynchronousEventProducer /*don't store it - it isn't a contextual reference*/)
    {
        if (injectionPoint.getMember() instanceof Field)
        {
            Class eventClass = (Class) ((ParameterizedType) ((Field) injectionPoint.getMember()).getGenericType()).getActualTypeArguments()[0];

            //don't use injectionPoint.getQualifiers() since we removed them during the bootstrapping
            List<Annotation> qualifiers = new ArrayList<Annotation>();
            for (Annotation annotation : ((Field)injectionPoint.getMember()).getAnnotations())
            {
                if (beanManager.isQualifier(annotation.annotationType()))
                {
                    qualifiers.add(annotation);
                }
            }

            if (qualifiers.isEmpty())
            {
                qualifiers.add(new DefaultLiteral());
            }
            eventClassAndQualifierHashCode = new BeanCacheKey(eventClass, qualifiers.toArray(new Annotation[qualifiers.size()])).hashCode();
        }
        else
        {
            throw new IllegalStateException("currently events are only supported via field-injection"); //TODO add more details about the usage
        }
        init(asynchronousEventProducer);
    }

    @Override
    public void fire(final Object event)
    {
        init(null);

        RingBuffer<DisruptorEventSlot> ringBuffer = ringBufferHolder.getRingBuffer();
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

    private void init(AsynchronousEventProducer asynchronousEventProducer)
    {
        if (this.ringBufferHolder == null)
        {
            lazyInit(asynchronousEventProducer);
        }
    }

    private synchronized void lazyInit(AsynchronousEventProducer asynchronousEventProducer)
    {
        if (this.ringBufferHolder != null)
        {
            return;
        }

        if (asynchronousEventProducer == null)
        {
            asynchronousEventProducer = BeanProvider.getContextualReference(AsynchronousEventProducer.class);
        }
        ConcurrentMap<Integer, RingBufferHolder> disruptorEntries = asynchronousEventProducer.getDisruptorEntries();
        List<DisruptorObserverEntry> disruptorObserverEntries = asynchronousEventProducer.getDisruptorExtension().getDisruptorObserver(this.eventClassAndQualifierHashCode);
        ContextControl contextControl = asynchronousEventProducer.getContextControl();
        this.ringBufferHolder = disruptorEntries.get(this.eventClassAndQualifierHashCode);

        if (ringBufferHolder == null)
        {
            AsynchronousEventConfig config = BeanProvider.getContextualReference(AsynchronousEventConfig.class);

            SlotEventHandler<DisruptorEventSlot>[] slotEventHandlers = new SlotEventHandler[disruptorObserverEntries.size()];

            for (int i = 0; i < disruptorObserverEntries.size(); i++)
            {
                slotEventHandlers[i] = new SlotEventHandler(disruptorObserverEntries.get(i));
            }

            ringBufferHolder = new RingBufferHolder(
                    config.getExecutor(),
                    config.getRingBufferSize(),
                    config.getProducerType(),
                    config.getWaitStrategy(),
                    contextControl,
                    slotEventHandlers);
            //TODO init
            RingBufferHolder existingRingBufferHolder = disruptorEntries.putIfAbsent(this.eventClassAndQualifierHashCode, ringBufferHolder);
            if (existingRingBufferHolder != null)
            {
                ringBufferHolder = existingRingBufferHolder;
            }
        }
    }
}
