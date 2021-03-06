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

import org.apache.deltaspike.cdise.api.ContextControl;
import org.os890.ds.addon.async.event.api.AsynchronousEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RequestScoped
public class AsynchronousEventProducer
{
    @Inject
    private BeanManager beanManager;

    @Inject
    private DisruptorExtension disruptorExtension;

    @Inject
    private ContextControl contextControl;

    private ConcurrentMap<Integer /*event-class/qualifier hash-code*/, RingBufferHolder> disruptorEntries = new ConcurrentHashMap<Integer, RingBufferHolder>();

    @Produces
    @Dependent
    protected AsynchronousEvent eventProxy(InjectionPoint injectionPoint)
    {
        return new AsynchronousEventProxy(beanManager, injectionPoint,
            this /*just to avoid an additional lookup - we need a ref. to an application-scoped storage for disruptorEntries in any case*/);
    }

    ConcurrentMap<Integer, RingBufferHolder> getDisruptorEntries()
    {
        return disruptorEntries;
    }

    DisruptorExtension getDisruptorExtension()
    {
        return disruptorExtension;
    }

    ContextControl getContextControl()
    {
        return contextControl;
    }
}
