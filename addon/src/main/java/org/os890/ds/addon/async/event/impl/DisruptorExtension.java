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

import org.apache.deltaspike.core.util.metadata.builder.ImmutableInjectionPoint;
import org.os890.ds.addon.async.event.api.AsynchronousEvent;
import org.os890.ds.addon.async.event.api.ObservesAsynchronous;
import org.os890.ds.addon.async.event.impl.util.BeanCacheKey;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * CDI extension that discovers methods annotated with {@link ObservesAsynchronous}
 * during bean processing and registers them as Disruptor-based asynchronous observers.
 */
public class DisruptorExtension implements Extension
{
    private List<ObserverEntry> disruptorObserverEntries = new ArrayList<ObserverEntry>();

    private Set<Annotation> defaultQualifier = new HashSet<Annotation>() {{
        add(Default.Literal.INSTANCE);
    }};

    protected void processAsyncEventSourcesAndTarges(@Observes ProcessBean<?> pb, BeanManager beanManager)
    {
        List<Method> foundMethods = new ArrayList<Method>(); //fallback
        Collections.addAll(foundMethods, pb.getBean().getBeanClass().getDeclaredMethods());

        reduceInjectionPointQualifiers(pb);
        
        if (pb.getAnnotated() instanceof AnnotatedType)
        {
            foundMethods.clear();
            for (AnnotatedMethod annotatedMethod : ((AnnotatedType<?>) pb.getAnnotated()).getMethods())
            {

                foundMethods.add(annotatedMethod.getJavaMember());
            }
        }

        for (Method method : foundMethods)
        {
            if (method.getParameterTypes().length == 1) //currently optional injection-points aren't supported
            {
                List<Annotation> qualifiers = new ArrayList<Annotation>();
                ObservesAsynchronous observesAsynchronous = null;

                for (Annotation annotation : method.getParameterAnnotations()[0])
                {
                    if (annotation.annotationType().equals(ObservesAsynchronous.class))
                    {
                        observesAsynchronous = (ObservesAsynchronous) annotation;
                    }
                    else if (beanManager.isQualifier(annotation.annotationType()))
                    {
                        qualifiers.add(annotation);
                    }
                }

                if (observesAsynchronous != null)
                {
                    if (!method.isAccessible())
                    {
                        method.setAccessible(true);
                    }
                    Class eventClass = method.getParameterTypes()[0];
                    if (qualifiers.isEmpty())
                    {
                        qualifiers.add(Default.Literal.INSTANCE);
                    }
                    int eventClassAndQualifierHashCode = new BeanCacheKey(eventClass, qualifiers.toArray(new Annotation[qualifiers.size()])).hashCode();
                    disruptorObserverEntries.add(new ObserverEntry(beanManager, pb.getBean(), method, eventClassAndQualifierHashCode));
                }
            }
        }
    }

    private void reduceInjectionPointQualifiers(ProcessBean<?> pb)
    {
        Iterator<InjectionPoint> injectionPointIterator = pb.getBean().getInjectionPoints().iterator();

        List<InjectionPoint> injectionPointsToRecreate = new ArrayList<InjectionPoint>();
        while (injectionPointIterator.hasNext())
        {
            InjectionPoint injectionPoint = injectionPointIterator.next();
            if (injectionPoint.getMember() instanceof Field && injectionPoint.getAnnotated() instanceof AnnotatedField &&
                AsynchronousEvent.class.isAssignableFrom(((Field)injectionPoint.getMember()).getType()))
            {
                if (injectionPoint.getQualifiers().size() != 1 ||
                    !Default.class.isAssignableFrom(injectionPoint.getQualifiers().iterator().next().annotationType()))
                {
                    if (injectionPoint.getQualifiers().size() > 0)
                    {
                        injectionPointsToRecreate.add(injectionPoint);
                        injectionPointIterator.remove();
                    }
                }
            }
        }

        for (InjectionPoint injectionPoint : injectionPointsToRecreate)
        {
            InjectionPoint recreatedInjectionPoint = new ImmutableInjectionPoint(
                (AnnotatedField)injectionPoint.getAnnotated(),
                this.defaultQualifier, //the default producer will handle the qualifier logic
                injectionPoint.getBean(),
                injectionPoint.isTransient(),
                injectionPoint.isDelegate());
            pb.getBean().getInjectionPoints().add(recreatedInjectionPoint);
        }
    }

    /**
     * Returns the list of observer entries matching the given event class and qualifier hash code.
     *
     * @param eventClassAndQualifierHashCode the combined hash code of the event class and its qualifiers
     * @return the matching observer entries, never {@code null}
     */
    public List<ObserverEntry> getDisruptorObserver(Integer eventClassAndQualifierHashCode)
    {
        List<ObserverEntry> result = new ArrayList<ObserverEntry>();
        for (ObserverEntry entry : this.disruptorObserverEntries)
        {
            if (entry.isEntryFor(eventClassAndQualifierHashCode))
            {
                result.add(entry);
            }
        }
        return result;
    }
}
