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

import org.apache.deltaspike.core.util.ExceptionUtils;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.reflect.Method;

public class DisruptorObserverEntry<T> {
    private final Class<T> eventClass;
    private final Bean<?> bean;
    private final Method observerMethod;
    private final BeanManager beanManager;

    public DisruptorObserverEntry(Class<T> eventClass, Bean<?> bean, Method observerMethod, BeanManager beanManager)
    {
        this.eventClass = eventClass;
        this.bean = bean;
        this.observerMethod = observerMethod;
        this.beanManager = beanManager;
    }

    public Class getEventClass()
    {
        return eventClass;
    }

    public void dispatch(T event)
    {
        //TODO alternative handling
        //TODO cache it
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);

        Object observerContextualInstance = beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
        try
        {
            observerMethod.invoke(observerContextualInstance, event);
        }
        catch (Exception e)
        {
            throw ExceptionUtils.throwAsRuntimeException(e);
        }
    }
}
