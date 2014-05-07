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

import org.os890.ds.addon.async.event.api.ObservesAsynchronous;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DisruptorExtension implements Extension
{
    private List<DisruptorObserverEntry> disruptorObserverEntries = new ArrayList<DisruptorObserverEntry>();

    protected void collectDisruptorConfig(@Observes ProcessBean pb, BeanManager beanManager)
    {
        for (Method method : pb.getBean().getBeanClass().getDeclaredMethods() /*TODO getMethods*/)
        {
            if (method.getParameterTypes().length == 1) //currently optional injection-points aren't supported
            {
                ObservesAsynchronous observesAsynchronous = null;

                for (Annotation annotation : method.getParameterAnnotations()[0])
                {
                    if (annotation.annotationType().equals(ObservesAsynchronous.class))
                    {
                        observesAsynchronous = (ObservesAsynchronous)annotation;
                    }
                }

                if (observesAsynchronous != null)
                {
                    Class eventClass = method.getParameterTypes()[0];
                    disruptorObserverEntries.add(new DisruptorObserverEntry(eventClass, pb.getBean(), method, beanManager));
                    break; //TODO currently we just support one observer per class
                }
            }
        }
    }

    public List<DisruptorObserverEntry> getDisruptorObserver(Class eventClass)
    {
        List<DisruptorObserverEntry> result = new ArrayList<DisruptorObserverEntry>();
        for (DisruptorObserverEntry entry : this.disruptorObserverEntries)
        {
            if (entry.getEventClass().equals(eventClass))
            {
                result.add(entry);
            }
        }
        return result;
    }
}
