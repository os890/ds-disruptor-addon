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
package org.os890.ds.addon.test.uc005;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.os890.ds.addon.async.event.api.AsynchronousEvent;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@RunWith(CdiTestRunner.class)
public class SimpleBenchmarkTest
{
    private static final Logger LOG = Logger.getLogger(SimpleBenchmarkTest.class.getName());
    private static final int SLEEP_TIME = 200;
    static final int EVENT_COUNT = 1000000;

    @Inject
    private AsynchronousEvent<TestEvent> myAsyncEvent;

    @Inject
    private EventObserver1 eventObserver1;

    @Inject
    private EventObserver2 eventObserver2;

    @Test
    public void compareWithNativePerformance() throws InterruptedException
    {
        double disruptorEventProcessingTime = nativeDisruptorBroadcast();

        final double expectedMax = disruptorEventProcessingTime * 1.2;
        double observerProcessingTime = asyncObserverBroadcast();

        String logMessage = ">>> native execution time: " + disruptorEventProcessingTime + "ms and observer execution time: " + observerProcessingTime + "ms";
        if (observerProcessingTime > expectedMax)
        {
            LOG.warning(logMessage);
            Assert.fail();
        }
        else
        {
            LOG.info(logMessage);
        }
    }

    private double nativeDisruptorBroadcast() throws InterruptedException
    {
        Disruptor<EventSlot<TestEvent>> disruptor = new Disruptor<EventSlot<TestEvent>>(new EventFactory<EventSlot<TestEvent>>()
        {
            @Override
            public EventSlot<TestEvent> newInstance()
            {
                return new EventSlot<TestEvent>();
            }
        }, 4096, Executors.newCachedThreadPool(), ProducerType.MULTI, new BlockingWaitStrategy());

        EventHandler1 eventHandler1 = new EventHandler1();
        EventHandler2 eventHandler2 = new EventHandler2();
        disruptor.handleEventsWith(eventHandler1, eventHandler2);
        RingBuffer<EventSlot<TestEvent>> ringBuffer = disruptor.start();

        TestEvent disruptorEvent = new TestEvent();

        //warmup
        for (int i = 0; i < EVENT_COUNT; i++)
        {
            publishEvent(ringBuffer, disruptorEvent);
        }
        Thread.sleep(SLEEP_TIME);

        eventHandler1.reset();
        eventHandler2.reset();

        long start = System.currentTimeMillis();
        for (int i = 0; i < EVENT_COUNT; i++)
        {
            publishEvent(ringBuffer, disruptorEvent);
        }
        Thread.sleep(SLEEP_TIME);
        return calculateEffectiveDuration(start, eventHandler1.getLastTouch(), eventHandler2.getLastTouch());
    }

    private double asyncObserverBroadcast() throws InterruptedException
    {
        //warmup
        TestEvent observerEvent = new TestEvent();
        for (int i = 0; i < EVENT_COUNT; i++)
        {
            this.myAsyncEvent.fire(observerEvent);
        }

        Thread.sleep(SLEEP_TIME);

        eventObserver1.reset();
        eventObserver2.reset();
        long start = System.currentTimeMillis();
        for (int i = 0; i < EVENT_COUNT; i++)
        {
            this.myAsyncEvent.fire(observerEvent);
        }
        Thread.sleep(SLEEP_TIME);
        return calculateEffectiveDuration (start, eventObserver1.getLastTouch(), eventObserver2.getLastTouch());
    }

    private double calculateEffectiveDuration(long start, long end1, long end2)
    {
        if (end1 == 0 || end2 == 0)
        {
            throw new IllegalArgumentException("invalid end time");
        }
        if (end1 > end2)
        {
            return end1 - start;
        }
        return end2 - start;
    }

    private void publishEvent(RingBuffer<EventSlot<TestEvent>> ringBuffer, TestEvent disruptorEvent)
    {
        long seq = ringBuffer.next();

        EventSlot<TestEvent> slot = ringBuffer.get(seq);
        slot.setEvent(disruptorEvent);
        ringBuffer.publish(seq);
    }
}
