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

package org.os890.ds.addon.async.event.api;

import java.io.Serializable;

/**
 * Represents an asynchronous event that can be fired via a LMAX Disruptor-backed ring buffer.
 * Inject this interface to dispatch events to methods annotated with {@link ObservesAsynchronous}.
 *
 * @param <E> the event payload type
 */
public interface AsynchronousEvent<E> extends Serializable
{
    /**
     * Publishes the given event to all registered asynchronous observers.
     *
     * @param event the event payload to dispatch
     */
    void fire(E event);
}
