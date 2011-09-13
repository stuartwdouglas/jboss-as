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

package org.jboss.as.server.suspend;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.jboss.as.server.ServerMessages;

/**
 * A suspend permit manager, responsible for issuing permits at a gateway into the application server.
 *
 * A gateway is considered to be anywhere that a request starts, and includes things like web, remote EJB, EJB timers
 * etc.
 *
 * One permit manager is created for each gateay, which allows for fine grained control of graceful shutdown for each
 * request type.
 *
 * @author Stuart Douglas
 */
public class SuspendPermitManager {

    private final String name;

    private static final AtomicLongFieldUpdater<SuspendPermitManager> OUTSTANDING_UPDATER = AtomicLongFieldUpdater.newUpdater(SuspendPermitManager.class, "outstandingPermitCount");

    private final Set<SuspendPermit> outstandingPermits = Collections.newSetFromMap(new ConcurrentHashMap<SuspendPermit, Boolean>(128));

    private volatile SuspendState suspendState = SuspendState.RUNNING;

    @SuppressWarnings("unused")
    private volatile long outstandingPermitCount = 0;

    /**
     * The shutdown callback. Should only be accessed under lock
     */
    private SuspendManager.ShutdownCompleteCallback callback;

    private final Object lock = new Object();

    public SuspendPermitManager(final String name) {
        this.name = name;
    }
    /**
     * Acquires a permit to allow a resource to do some work. IF the server is shutting down the permit will
     * not be granted.
     *
     * @return The acquired permit
     * @throws ServerSuspendingException If the server is suspending or suspended
     */
    public SuspendPermit acquirePermit() throws ServerSuspendingException {
        //increment before reading the suspend state
        //the outstanding count is read after the suspended state is changed
        OUTSTANDING_UPDATER.incrementAndGet(this);
        if (this.suspendState != SuspendState.RUNNING) {
            synchronized (lock) {
                if (this.suspendState != SuspendState.RUNNING) {
                    //turns out we are not eligible for a permit
                    //decrement the count
                    long outstanding = OUTSTANDING_UPDATER.decrementAndGet(this);
                    if(outstanding == 0) {
                        lock.notifyAll();
                    }
                    throw new ServerSuspendingException();
                }
            }
        }
        final SuspendPermit permit = new SuspendPermit(name);
        outstandingPermits.add(permit);
        return permit;
    }

    /**
     * Releases a granted permit. When the server is suspending once all granted permits have been released the
     * server will enter a suspended state.
     *
     * @param permit The permit to release
     */
    public void releasePermit(final SuspendPermit permit) {
        if(!outstandingPermits.remove(permit)) {
            throw ServerMessages.MESSAGES.permitReturnedTwice(permit);
        }
        long outstanding = OUTSTANDING_UPDATER.decrementAndGet(this);
        if (suspendState != SuspendState.RUNNING) {
            synchronized (lock) {
                if (suspendState != SuspendState.RUNNING) {
                    if(outstanding == 0) {
                        callback.shutdownComplete();
                        callback = null;
                        suspendState = SuspendState.SUSPENDED;
                    }
                }
            }
        }
    }

    /**
     * Commence the shutdown process. No new requests should be accepted.
     * <p/>
     * If there are currently no requests active then then method should return true. Otherwise it should use the
     * callback to notify the manager that it's shutdown has completed.
     *
     * @param callback The callback that must be called when the manager is shut down
     *
     * @return true if the manager can be shut down immediately
     */
    boolean shutdown(final SuspendManager.ShutdownCompleteCallback callback) {
        synchronized (lock) {
            this.callback = callback;
            this.suspendState = SuspendState.SUSPENDING;
            if (OUTSTANDING_UPDATER.get(this) == 0) {

                //even though activeThreads can be modified when not under lock, we still get the correct behaviour
                //this is because now that shutdown is true, any modification will result in an entry to
                //the callbackIfNecessary synchronized block
                //it is possible for callbackIfNecessary to be called multiple times when activeThreads are empty
                this.callback = null;
                this.suspendState = SuspendState.SUSPENDED;
                //we return true as there was no outstanding threads
                return true;
            }
            return false;
        }
    }

    /**
     * Resume normal operations. This can be called both during a shutdown, and after a shutdown has taken place.
     *
     */
    void resume() {
        synchronized (lock) {
            suspendState = SuspendState.RUNNING;
            callback = null;
        }
    }


    /**
     *
     * @return The name of this permit manager. Permit managers may be referenced by name
     * in the management API
     */
    public String getName() {
        return name;
    }

}
