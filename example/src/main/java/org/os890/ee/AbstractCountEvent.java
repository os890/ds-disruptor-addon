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
