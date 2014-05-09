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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.lang.reflect.Method;

class ObserverEntry<T>
{
    private final int eventClassAndQualifierHashCode;
    private final Bean<?> bean;
    private final Method observerMethod;
    private final BeanManager beanManager;
    private final Object observerContextualReference;

    ObserverEntry(BeanManager beanManager,
                  Bean<?> bean,
                  Method observerMethod,
                  int eventClassAndQualifierHashCode)
    {
        this.observerMethod = observerMethod;
        this.beanManager = beanManager;
        this.eventClassAndQualifierHashCode = eventClassAndQualifierHashCode;

        if (beanManager.isNormalScope(bean.getScope()))
        {
            this.observerContextualReference = beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean));
            this.bean = null;
        }
        else
        {
            this.observerContextualReference = null;
            this.bean = bean;
        }
    }

    void dispatch(T event)
    {
        //TODO alternative handling
        //TODO cache it

        final Object currentObserverContextualReference;
        CreationalContext<Object> creationalContext = null;

        if (observerContextualReference != null)
        {
            currentObserverContextualReference = observerContextualReference;
        }
        else
        {
            creationalContext = beanManager.createCreationalContext((Bean<Object>)bean);
            currentObserverContextualReference = beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
            if (!Dependent.class.isAssignableFrom(bean.getScope()))
            {
                creationalContext = null;
            }
        }

        try
        {
            observerMethod.invoke(currentObserverContextualReference, event);
        }
        catch (Exception e)
        {
            throw ExceptionUtils.throwAsRuntimeException(e);
        }
        finally
        {
            if (creationalContext != null)
            {
                ((Bean<Object>)bean).destroy(currentObserverContextualReference, creationalContext);
            }
        }
    }

    public boolean isEntryFor(int hashCode)
    {
        return this.eventClassAndQualifierHashCode == hashCode;
    }
}
