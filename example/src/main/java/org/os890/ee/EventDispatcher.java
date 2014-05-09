/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package org.os890.ee;

import org.apache.deltaspike.core.api.scope.WindowScoped;
import org.os890.ds.addon.async.event.api.AsynchronousEvent;
import org.os890.ee.observer.async.disruptor.DemoDisruptorEvent;
import org.os890.ee.observer.async.disruptor.DisruptorBasedEventObserver1;
import org.os890.ee.observer.async.disruptor.DisruptorBasedEventObserver2;
import org.os890.ee.observer.async.ejb.stateful.DemoStatefulEjbEvent;
import org.os890.ee.observer.async.ejb.stateful.StatefulEjbEventObserver1;
import org.os890.ee.observer.async.ejb.stateful.StatefulEjbEventObserver2;
import org.os890.ee.observer.async.ejb.stateless.DemoStatelessEjbEvent;
import org.os890.ee.observer.async.ejb.stateless.StatelessEjbEventObserver1;
import org.os890.ee.observer.async.ejb.stateless.StatelessEjbEventObserver2;
import org.os890.ee.observer.cdi.CdiEventObserver1;
import org.os890.ee.observer.cdi.CdiEventObserver2;
import org.os890.ee.observer.cdi.DemoCdiEvent;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named
@WindowScoped
public class EventDispatcher implements Serializable
{
    private int eventCount = 1000000;
    private int sleepTime = 200;
    private int ejbSleepTime = 5000;

    /*
     * disruptor based events/observer
     */
    @Inject
    private AsynchronousEvent<DemoDisruptorEvent> asynchronousEvent;

    @Inject
    private DisruptorBasedEventObserver1 disruptorBasedEventObserver1;

    @Inject
    private DisruptorBasedEventObserver2 disruptorBasedEventObserver2;

    /*
     * std cdi events/observer
     */
    @Inject
    private Event<DemoCdiEvent> cdiEvent;

    @Inject
    private CdiEventObserver1 cdiEventObserver1;

    @Inject
    private CdiEventObserver2 cdiEventObserver2;

    /*
     * async stateful ejb events/observer
     */
    @Inject
    private Event<DemoStatefulEjbEvent> statefulEjbEvent;

    @Inject
    private StatefulEjbEventObserver1 statefulEjbEventObserver1;

    @Inject
    private StatefulEjbEventObserver2 statefulEjbEventObserver2;

    /*
     * async stateless ejb events/observer
     */
    @Inject
    private Event<DemoStatelessEjbEvent> statelessEjbEvent;

    @Inject
    private StatelessEjbEventObserver1 statelessEjbEventObserver1;

    @Inject
    private StatelessEjbEventObserver2 statelessEjbEventObserver2;

    private long start;

    private long asyncDisruptorBasedEventResult;
    private long cdiEventResult;
    private long statefulEjbEventResult;
    private long statelessEjbEventResult;

    public void sendViaAsyncEvent() throws InterruptedException
    {
        DemoDisruptorEvent event = new DemoDisruptorEvent();
        event.reset(eventCount);

        //warmup
        for (int i = 0; i < eventCount; i++)
        {
            asynchronousEvent.fire(event);
        }

        Thread.sleep(sleepTime);
        event.reset(eventCount);
        start = System.currentTimeMillis();
        for (int i = 0; i < eventCount; i++)
        {
            asynchronousEvent.fire(event);
        }
        Thread.sleep(sleepTime);

        asyncDisruptorBasedEventResult = event.getLastTouch() - start;
    }

    public void sendStdCdiEvent() throws InterruptedException
    {
        DemoCdiEvent event = new DemoCdiEvent();
        event.reset(eventCount);

        //warmup
        for (int i = 0; i < eventCount; i++)
        {
            cdiEvent.fire(event);
        }

        //sync delivery -> no sleep

        event.reset(eventCount);
        start = System.currentTimeMillis();
        for (int i = 0; i < eventCount; i++)
        {
            cdiEvent.fire(event);
        }
        //sync delivery -> no sleep
        cdiEventResult = event.getLastTouch() - start;
    }

    public void sendAsyncEventToStatefulEjb() throws InterruptedException
    {
        DemoStatefulEjbEvent event = new DemoStatefulEjbEvent();
        event.reset(eventCount);

        //warmup
        for (int i = 0; i < eventCount; i++)
        {
            statefulEjbEvent.fire(event);
        }
        Thread.sleep(ejbSleepTime);

        event.reset(eventCount);
        start = System.currentTimeMillis();
        for (int i = 0; i < eventCount; i++)
        {
            statefulEjbEvent.fire(event);
        }
        Thread.sleep(ejbSleepTime);

        statefulEjbEventResult = event.getLastTouch() - start;
    }

    public void sendAsyncEventToStatelessEjb() throws InterruptedException
    {
        DemoStatelessEjbEvent event = new DemoStatelessEjbEvent();
        event.reset(eventCount);

        //warmup
        for (int i = 0; i < eventCount; i++)
        {
            statelessEjbEvent.fire(event);
        }
        Thread.sleep(ejbSleepTime);

        event.reset(eventCount);
        start = System.currentTimeMillis();
        for (int i = 0; i < eventCount; i++)
        {
            statelessEjbEvent.fire(event);
        }
        Thread.sleep(ejbSleepTime);

        statelessEjbEventResult = event.getLastTouch() - start;
    }

    public int getSleepTime()
    {
        return sleepTime;
    }

    public void setSleepTime(int sleepTime)
    {
        this.sleepTime = sleepTime;
    }

    public long getCdiEventResult()
    {
        return cdiEventResult;
    }

    public long getAsyncDisruptorBasedEventResult()
    {
        return asyncDisruptorBasedEventResult;
    }

    public long getStatefulEjbEventResult()
    {
        return statefulEjbEventResult;
    }

    public long getStatelessEjbEventResult()
    {
        return statelessEjbEventResult;
    }

    public int getEventCount()
    {
        return eventCount;
    }

    public void setEventCount(int eventCount)
    {
        this.eventCount = eventCount;
    }
}
