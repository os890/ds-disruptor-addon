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

package org.os890.ee;

import org.apache.deltaspike.core.util.ExceptionUtils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractCountEvent
{
    protected AtomicInteger count1 = new AtomicInteger();
    protected AtomicInteger count2 = new AtomicInteger();
    protected AtomicLong lastTouch = new AtomicLong(0);
    protected int eventCount;

    public void touch1()
    {
        if (count1.incrementAndGet() == eventCount)
        {
            lastTouch.set(System.currentTimeMillis());
        }
    }

    public void touch2()
    {
        count2.incrementAndGet();
    }

    public void reset(int eventCount)
    {
        this.eventCount = eventCount;
        lastTouch.set(0);
        count1.set(0);
        count2.set(0);
    }

    public long getLastTouch()
    {
        if (count1.get() != count2.get())
        {
            try
            {
                Thread.sleep(1000); //sometimes it takes a bit longer
            }
            catch (InterruptedException e)
            {
                throw ExceptionUtils.throwAsRuntimeException(e);
            }
        }
        if (count1.get() != count2.get())
        {
            throw new IllegalStateException("count 1: " + count1.get() + " count 2: " + count2.get());
        }
        return lastTouch.get();
    }
}
