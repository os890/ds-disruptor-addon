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
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.util.ExceptionUtils;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import java.util.concurrent.atomic.AtomicBoolean;

//based on BatchEventProcessor
class CdiAwareEventProcessor<E> implements EventProcessor
{
    private final ContextControl contextControl;
    private final RingBuffer<DisruptorEventSlot<E>> ringBuffer;
    private final EventHandler<DisruptorEventSlot<E>> eventHandler;
    private final SequenceBarrier barrier;

    private final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    private final AtomicBoolean running = new AtomicBoolean(false);

    CdiAwareEventProcessor(ContextControl contextControl,
                           RingBuffer<DisruptorEventSlot<E>> ringBuffer,
                           EventHandler<DisruptorEventSlot<E>> eventHandler,
                           SequenceBarrier barrier)
    {
        this.contextControl = contextControl;
        this.ringBuffer = ringBuffer;
        this.eventHandler = eventHandler;
        this.barrier = barrier;
    }

    @Override
    public Sequence getSequence()
    {
        return sequence;
    }

    @Override
    public void halt()
    {
        running.set(false);
        barrier.alert();
    }

    @Override
    public boolean isRunning()
    {
        return running.get();
    }

    @Override
    public void run()
    {
        try
        {
            contextControl.startContext(RequestScoped.class);
            contextControl.startContext(SessionScoped.class);

            if (!running.compareAndSet(false, true))
            {
                throw new IllegalStateException("Thread exists already");
            }
            barrier.clearAlert();

            notifyStart();

            DisruptorEventSlot<E> event;
            long nextSequence = sequence.get() + 1L;
            try
            {
                while (true)
                {
                    try
                    {
                        final long availableSequence = barrier.waitFor(nextSequence);

                        while (nextSequence <= availableSequence)
                        {
                            event = ringBuffer.get(nextSequence);
                            eventHandler.onEvent(event, nextSequence, nextSequence == availableSequence);
                            nextSequence++;
                        }

                        sequence.set(availableSequence);
                    }
                    catch (final TimeoutException e)
                    {
                        //TODO ds-exception handling
                        throw ExceptionUtils.throwAsRuntimeException(e);
                    }
                    catch (final AlertException ex)
                    {
                        if (!running.get())
                        {
                            break;
                        }
                    }
                    catch (final Throwable ex)
                    {
                        //TODO DS exception handling
                        sequence.set(nextSequence);
                        nextSequence++;
                    }
                }
            }
            finally
            {
                notifyShutdown();
                running.set(false);
            }
        }
        finally
        {
            contextControl.stopContext(SessionScoped.class);
            contextControl.stopContext(RequestScoped.class);
        }
    }

    private void notifyStart()
    {
        if (eventHandler instanceof LifecycleAware)
        {
            try
            {
                ((LifecycleAware) eventHandler).onStart();
            }
            catch (final Throwable ex)
            {
                //TODO ds-exception handling
                throw ExceptionUtils.throwAsRuntimeException(ex);
            }
        }
    }

    private void notifyShutdown()
    {
        if (eventHandler instanceof LifecycleAware)
        {
            try
            {
                ((LifecycleAware) eventHandler).onShutdown();
            }
            catch (final Throwable ex)
            {
                //TODO ds-exception handling
                throw ExceptionUtils.throwAsRuntimeException(ex);
            }
        }
    }
}
