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

import com.lmax.disruptor.EventHandler;

/**
 * Bridges disruptor event to observer-method
 */
class EventSlotHandler<E> implements EventHandler<EventSlot<E>>
{
    private final ObserverEntry<E> observerEntry;

    EventSlotHandler(ObserverEntry<E> observerEntry)
    {
        this.observerEntry = observerEntry;
    }

    @Override
    public void onEvent(EventSlot<E> event, long sequence, boolean endOfBatch) throws Exception
    {
        observerEntry.dispatch(event.getEvent());
    }
}
