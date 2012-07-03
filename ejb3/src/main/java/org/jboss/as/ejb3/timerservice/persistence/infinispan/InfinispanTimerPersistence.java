/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.timerservice.persistence.infinispan;

import java.util.ArrayList;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.timerservice.persistence.TimerEntity;
import org.jboss.as.ejb3.timerservice.persistence.TimerListener;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class InfinispanTimerPersistence implements TimerPersistence, Service<InfinispanTimerPersistence> {

    private final InjectedValue<EmbeddedCacheManager> cacheContainerInjectedValue = new InjectedValue<EmbeddedCacheManager>();
    private final List<TimerListener> listeners = new ArrayList<TimerListener>();
    private final String cacheName;
    private volatile Cache<String, TimerEntity> cache;
    private final InfinispanListener infinispanListener = new InfinispanListener();

    public InfinispanTimerPersistence(final String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public void start(final StartContext context) throws StartException {

        final EmbeddedCacheManager embeddedCacheManager = cacheContainerInjectedValue.getValue();
        final Cache<String, TimerEntity> cache = embeddedCacheManager.getCache(cacheName, true);
        cache.addListener(infinispanListener);
        this.cache = cache;
        if(embeddedCacheManager.getDefaultCacheConfiguration().eviction().strategy() != EvictionStrategy.NONE) {
            throw EjbMessages.MESSAGES.infinispanShouldNotHaveEvictionConfigured(cacheName);
        }

    }

    @Override
    public void stop(final StopContext context) {
        cache.removeListener(infinispanListener);
        this.cache = null;
        cacheContainerInjectedValue.getValue().removeCache(cacheName);
    }

    @Override
    public void addTimer(final TimerEntity timerEntity) {
        cache.put(cacheKey(timerEntity), timerEntity);
    }

    @Override
    public void persistTimer(final TimerEntity timerEntity) {
        cache.put(cacheKey(timerEntity), timerEntity);
    }

    @Override
    public void timerUndeployed(final String timedObjectId) {

    }

    @Override
    public TimerEntity loadTimer(final String id, final String timedObjectId) {
        return cache.get(cacheKey(timedObjectId, id));
    }

    @Override
    public List<TimerEntity> loadActiveTimers(final String timedObjectId, final Object primaryKey) {
        final List<TimerEntity> timers = new ArrayList<TimerEntity>();
        for (TimerEntity i : cache.values()) {
            if (primaryKey == null || primaryKey.equals(i.getPrimaryKey())) {
                timers.add(i);
            }
        }
        return new ArrayList<TimerEntity>(cache.values());
    }

    @Override
    public List<TimerEntity> loadActiveTimers(final String timedObjectId) {
        return new ArrayList<TimerEntity>(cache.values());
    }

    @Override
    public void addListener(final TimerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(final TimerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public InfinispanTimerPersistence getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private String cacheKey(final TimerEntity timerEntity) {
        return cacheKey(timerEntity.getTimedObjectId(), timerEntity.getId());
    }

    private String cacheKey(final String timedObjectId, final String timerId) {
        return timedObjectId + "-" + timerId;
    }

    @Listener(sync = true)
    private final class InfinispanListener {

        @CacheEntryCreated
        public void cacheEntryCreated(CacheEntryCreatedEvent<String, TimerEntity> event) {
            final TimerEntity timer = cache.get(event.getKey());
            synchronized (listeners) {
                for (TimerListener listener : listeners) {
                    listener.timerAdded(timer);
                }
            }
        }

        @CacheEntryRemoved
        public void cacheEntryRemoved(final CacheEntryRemovedEvent<String, TimerEntity> event) {
            synchronized (listeners) {
                for (TimerListener listener : listeners) {
                    listener.timerRemoved(event.getValue());
                }
            }
        }
    }

    public InjectedValue<EmbeddedCacheManager> getCacheContainerInjectedValue() {
        return cacheContainerInjectedValue;
    }
}
