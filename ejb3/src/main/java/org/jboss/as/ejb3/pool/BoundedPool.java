/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.pool;

import org.jboss.as.ejb3.logging.EjbLogger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * An EJB pool that takes two parameters, max instances and pool size.
 *
 * Max instances controls the maximum number of total instances that may be created.
 * Pool size controls the maximum number of instances that can be stored in the pool.
 *
 * If pool size is zero then pooling is disabled
 * If max instances is zero then the maximum number of instances is unbounded
 *
 *
 * @author Stuart Douglas
 */
public class BoundedPool<T> implements Pool<T> {

    private volatile int maxInstances;
    private final int poolSize;
    private final long timeout;
    private final StatelessObjectFactory<T> factory;
    private final boolean statisticsEnabled;
    private final Object lock = new Object();


    /**
     * The number of currently created instances, used to enforce the maxInstances limit.
     *
     * If the maxInstances limit is not set this is not used
     */
    @SuppressWarnings("unused")
    private volatile int instances;

    /**
     * The current size of the concurrent queue if pooling is in use. Managed independently so CAS operations
     * can be used to control the size of the queue.
     */
    @SuppressWarnings("unused")
    private volatile int pooledInstances;

    //only accessed under lock
    private int waiters;

    private static AtomicIntegerFieldUpdater<BoundedPool> instancesUpdater = AtomicIntegerFieldUpdater.newUpdater(BoundedPool.class, "instances");
    private static AtomicIntegerFieldUpdater<BoundedPool> pooledInstancesUpdater = AtomicIntegerFieldUpdater.newUpdater(BoundedPool.class, "pooledInstances");


    private final Queue<T> pool;

    //statistics and their updaters
    @SuppressWarnings("unused")
    private volatile int removeCount;
    @SuppressWarnings("unused")
    private volatile int createCount;
    private static final AtomicIntegerFieldUpdater<BoundedPool> removeCountUpdater = AtomicIntegerFieldUpdater.newUpdater(BoundedPool.class, "removeCount");
    private static final AtomicIntegerFieldUpdater<BoundedPool> createCountUpdater = AtomicIntegerFieldUpdater.newUpdater(BoundedPool.class, "createCount");

    public BoundedPool(StatelessObjectFactory<T> factory, int maxInstances, int poolSize, long timeout, TimeUnit timeUnit, boolean statisticEnabled) {
        this.maxInstances = maxInstances;
        this.poolSize = poolSize;
        this.timeout = timeUnit.toMillis(timeout);
        this.factory = factory;
        this.statisticsEnabled = statisticEnabled;
        if(poolSize > 0) {
            pool = new ConcurrentLinkedQueue<>();
        } else {
            pool = null;
        }
    }


    @Override
    public void discard(T obj) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Discard instance %s#%s", this, obj);
        }
        if(maxInstances > 0) {
            if(instancesUpdater.decrementAndGet(this) == maxInstances - 1) {
                //we were at max size, now we are not, it is possible that there are waiters
                synchronized (lock) {
                    if(waiters > 0) {
                        lock.notifyAll();
                    }
                }
            }
        }
        doRemove(obj);
    }

    private void doRemove(T obj) {
        try {
            factory.destroy(obj);
        } finally {
            if(statisticsEnabled) {
                removeCountUpdater.incrementAndGet(this);
            }
        }
    }

    @Override
    public T get() {
        long endTime = -1;
        while (true) {
            //first try and grab something from the pool
            if (poolSize > 0) {
                int sz;
                boolean ok;
                do {
                    sz = pooledInstancesUpdater.get(this);
                    ok = (sz > 0);
                    if (!ok) {
                        //pool is empty :-(
                        break;
                    }
                } while (!pooledInstancesUpdater.compareAndSet(this, sz, sz - 1));
                if (ok) {
                    return pool.poll();
                }
            }
            //otherwise try and create a new one, if we are allowed
            boolean canCreate = false;
            if (maxInstances > 0) {
                int sz;
                //fandangled CAS code to make sure we don't exceed max instances

                do {
                    sz = instancesUpdater.get(this);
                    canCreate = (sz != maxInstances);
                    if (!canCreate) {
                        //we are at max size
                        break;
                    }
                } while (!instancesUpdater.compareAndSet(this, sz, sz + 1));
                if (!canCreate) {
                    //we can't create, so we wait
                    synchronized (lock) {
                        //we need to re-check in the sync block
                        //otherwise an instance may have been destroyed or released into the pool in the mean time
                        int recheck = instancesUpdater.get(this);
                        int poolInstances = pooledInstancesUpdater.get(this);
                        if (recheck == maxInstances && poolInstances == 0) {
                            //still at max size, and nothing pooled increment the waiters
                            waiters++;
                            try {
                                if (timeout > 0) {
                                    //if we have a timeout then we need to figure this out, as there may be multiple
                                    long remaining;
                                    if (endTime < 0) {
                                        endTime = System.currentTimeMillis() + timeout;
                                        remaining = timeout;
                                    } else {
                                        remaining = System.currentTimeMillis() - endTime;
                                        if (remaining <= 0) {
                                            throw EjbLogger.ROOT_LOGGER.failedToAcquirePermit(timeout, TimeUnit.MILLISECONDS);
                                        }
                                    }
                                    lock.wait(remaining);
                                } else {
                                    lock.wait();
                                }
                            } catch (InterruptedException e) {
                                throw EjbLogger.ROOT_LOGGER.acquireSemaphoreInterrupted();
                            } finally {
                                waiters--;
                            }
                        }
                    }
                }
            }
            if(canCreate) {
                if(statisticsEnabled) {
                    createCountUpdater.incrementAndGet(this);
                }
                return factory.create();
            }
        }

    }

    @Override
    public int getAvailableCount() {
        if(maxInstances > 0) {
            return maxInstances - instancesUpdater.get(this);
        }
        return 0;
    }

    @Override
    public int getCreateCount() {
        return createCount;
    }

    @Override
    public int getCurrentSize() {
        return getCreateCount() - getRemoveCount();
    }

    @Override
    public int getMaxSize() {
        return maxInstances;
    }

    @Override
    public int getRemoveCount() {
        return removeCountUpdater.get(this);
    }

    @Override
    public void release(T obj) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Releasing instance %s#%s", this, obj);
        }
        if(poolSize > 0) {
            //we may need to add this object to the pool
            int sz;
            boolean pool = true;
            do {
                sz = pooledInstancesUpdater.get(this);
                if(sz == poolSize) {
                    pool = false;
                    break;
                }
            } while (!pooledInstancesUpdater.compareAndSet(this, sz, sz + 1));
            if(pool) {
                this.pool.add(obj);
                //we may need to notify waiters, lets check
                if(maxInstances > 0) {
                    if(instancesUpdater.get(this) == maxInstances) {
                        //we were at max size, now we are not, it is possible that there are waiters
                        synchronized (lock) {
                            if(waiters > 0) {
                                lock.notifyAll();
                            }
                        }
                    }
                }
            } else {
                try {
                    doRemove(obj);
                } finally {
                    //if we are tracking instances decrease the active count and notify waiters
                    if(maxInstances > 0) {
                        if(instancesUpdater.decrementAndGet(this) == maxInstances - 1) {
                            //we were at max size, now we are not, it is possible that there are waiters
                            synchronized (lock) {
                                if(waiters > 0) {
                                    lock.notifyAll();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setMaxSize(int maxSize) {
        this.maxInstances = maxSize;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        for (T obj = pool.poll(); obj != null; obj = pool.poll()) {
            doRemove(obj);
        }
    }
}
