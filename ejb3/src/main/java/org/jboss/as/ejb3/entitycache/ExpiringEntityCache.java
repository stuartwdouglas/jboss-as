/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.entitycache;

import org.jboss.logging.Logger;

import javax.ejb.NoSuchEJBException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cache that expires entities if they have not been accessed within a specified timeout
 *
 * @author Stuart Douglas
 */
public class ExpiringEntityCache<T> implements EntityCache<T> {

    private static final long SLEEP_TIME = 500;

    private final long millisecondTimeout;
    private final String beanName;
    private final Map<Object, Entry> cache;

    private final EntityObjectFactory<T> factory;
    private volatile ExpirationTask expiryThread;

    private static final Logger logger = Logger.getLogger(ExpiringEntityCache.class);

    private class ExpirationTask extends Thread {

        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {

                final List<Entry> queue = new ArrayList<Entry>();
                final long time = System.currentTimeMillis();
                synchronized (cache) {
                    Iterator<Map.Entry<Object, Entry>> iterator = cache.entrySet().iterator();
                    while (iterator.hasNext()) {
                        final Map.Entry<Object, Entry> entry = iterator.next();
                        if (entry.getValue().isExpired(time)) {
                            queue.add(entry.getValue());
                            iterator.remove();
                        }
                    }
                }
                for (Entry value : queue) {
                    try {
                        logger.debugf("Passivating entity bean %s - %s as it has been inactive for %d milliseconds", beanName, value.getPrimaryKey(), millisecondTimeout);
                        factory.destroyInstance(value.getValue());
                    } catch (Exception e) {
                        logger.error("Exception passivating entity bean " + value.getPrimaryKey(), e);
                    }
                }

                try {
                    sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    running = false;
                    return;
                }
            }
        }

        public void stopTask() {
            running = false;
        }
    }

    private enum State {
        IN_USE, INACTIVE
    }

    private final class Entry {
        private long lastUsed;
        private State state = State.IN_USE;
        private final T value;
        private final Object primaryKey;

        public Entry(final Object primaryKey, final T value) {
            this.primaryKey = primaryKey;
            this.value = value;
            this.lastUsed = System.currentTimeMillis();
        }

        public Object getPrimaryKey() {
            return primaryKey;
        }

        public boolean isExpired(long currentTimeInMillis) {
            return state == State.INACTIVE && currentTimeInMillis > lastUsed + millisecondTimeout;
        }

        public T getValue() {
            return value;
        }
    }

    public ExpiringEntityCache(final EntityObjectFactory<T> factory, long value, TimeUnit timeUnit, final String beanName) {
        this.factory = factory;
        this.beanName = beanName;
        millisecondTimeout = TimeUnit.MILLISECONDS.convert(value, timeUnit);
        cache = new HashMap<Object, Entry>();
    }

    @Override
    public T get(final Object key) throws NoSuchEJBException {
        synchronized (cache) {
            Entry val = cache.get(key);
            if (val == null) {
                T instance = factory.createInstance(key);
                val = new Entry(key, instance);
            }
            val.lastUsed = System.currentTimeMillis();
            val.state = State.IN_USE;
            return val.getValue();
        }
    }

    @Override
    public void discard(final Object primaryKey) {
        synchronized (cache) {
            cache.remove(primaryKey);
        }
    }

    @Override
    public void create(Object key, final T instance) throws NoSuchEJBException {
        synchronized (cache) {
            Entry val = new Entry(key, instance);
            val.lastUsed = System.currentTimeMillis();
            val.state = State.INACTIVE;
            cache.put(key, val);
        }
    }

    @Override
    public void release(final Object primaryKey) {
        if (millisecondTimeout > 0) {
            synchronized (cache) {
                Entry entry = cache.get(primaryKey);

                if (entry == null) {
                    logger.warn("Could not find entity bean to release " + primaryKey);
                    return;
                }
                //this must stay within the synchronized block so the changes are visible to other threads
                entry.lastUsed = System.currentTimeMillis();
                entry.state = State.INACTIVE;
            }
        } else {
            //if there is no expiry, then just release back to the pool straight away
            remove(primaryKey);
        }
    }

    @Override
    public void remove(final Object key) {
        Entry object;

        synchronized (cache) {
            object = cache.remove(key);
        }

        factory.destroyInstance(object.value);
    }


    @Override
    public synchronized void start() {
        if (millisecondTimeout >= 0) {
            expiryThread = new ExpirationTask();
            expiryThread.setDaemon(true);
            expiryThread.start();
            expiryThread.setName("Expiry Thread for SFSB " + beanName);
        }
    }

    @Override
    public synchronized void stop() {
        if (expiryThread != null) {
            expiryThread.stopTask();
        }
        synchronized (cache) {
            cache.clear();
        }
    }
}
