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
package org.jboss.as.server.suspend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.msc.service.ServiceName;

import static org.jboss.as.server.ServerLogger.AS_ROOT_LOGGER;

/**
 * Co-ordinator for suspending and resuming operations on the server. Before a graceful shutdown this will wait for all
 * server activity to cease, and then shutdown will commence.
 *
 * @author Stuart Douglas
 */
public class SuspendManager {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("suspendManager");

    private final List<SuspendPermitManager> permitManagers = new ArrayList<SuspendPermitManager>();
    private final List<SuspendListener> listeners = new ArrayList<SuspendListener>();

    private Map<ShutdownCompleteCallback, SuspendPermitManager> outstandingManagers;

    private volatile SuspendState suspendState;

    /**
     * Begin the suspension process for a deployment
     */
    public synchronized void suspend() {
        if (suspendState == SuspendState.SUSPENDING) {
            AS_ROOT_LOGGER.suspendIgnoredAlreadyInProgress();
            return;
        } else if (suspendState == SuspendState.SUSPENDED) {
            AS_ROOT_LOGGER.suspendIgnoredAlreadySuspended();
            return;
        }
        AS_ROOT_LOGGER.suspendingServerOperations();

        //notify the listeners
        for (final SuspendListener listener : listeners) {
            try {
                listener.suspendStarted();
            } catch (Exception e) {
                AS_ROOT_LOGGER.suspendListenerThrewException(e, listener.toString(), "shutdownStarted");
            }
        }

        //shut down all the managers
        suspendState = SuspendState.SUSPENDING;
        final Map<ShutdownCompleteCallback, SuspendPermitManager> outstandingManagers = new HashMap<ShutdownCompleteCallback, SuspendPermitManager>();
        for (SuspendPermitManager permitManager : permitManagers) {
            try {
                final ShutdownCompleteCallback callBack = new ShutdownCompleteCallback();
                if (!permitManager.shutdown(callBack)) {
                    outstandingManagers.put(callBack, permitManager);
                }
            } catch (Exception e) {
                AS_ROOT_LOGGER.couldNotShutDownPermitManager(e, permitManager.getName());
            }
        }

        if (outstandingManagers.isEmpty()) {
            //there were no threads active, the deployment can be shut down immediately
            AS_ROOT_LOGGER.serverSuspended();
            finishSuspend();
        } else {
            this.outstandingManagers = outstandingManagers;
        }
    }

    /**
     * Resume normal server operations. This may be called while the server is in the process of being suspended,
     * which will abort the suspend operation.
     */
    public synchronized void resume() {

        suspendState = SuspendState.RUNNING;
        outstandingManagers = null;

        for (final SuspendPermitManager manager : permitManagers) {
            try {
                manager.resume();
            } catch (Exception e) {
                AS_ROOT_LOGGER.couldNotResumePermitManager(e, manager.getName());
            }
        }

        //notify the listeners
        for (final SuspendListener listener : listeners) {
            try {
                listener.resumed();
            } catch (Exception e) {
                AS_ROOT_LOGGER.suspendListenerThrewException(e, listener.toString(), "resumed");
            }
        }
    }

    public synchronized void addListener(final SuspendListener listener) {
        if (listener.listenerAdded(suspendState)) {
            this.listeners.add(listener);
        }
    }

    public synchronized void removeListener(final SuspendListener listener) {
        this.listeners.remove(listener);
    }

    public synchronized void addPermitManager(final SuspendPermitManager manager) {
        manager.start(this);
        this.permitManagers.add(manager);
    }

    public synchronized void removePermitManager(final SuspendPermitManager manager) {
        manager.stop();
        this.permitManagers.remove(manager);
        //if the manager is removed while the server is shutting down
        //we want to ignore it from now on
        if (outstandingManagers != null) {
            Iterator<Map.Entry<ShutdownCompleteCallback, SuspendPermitManager>> it = outstandingManagers.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<ShutdownCompleteCallback, SuspendPermitManager> entry = it.next();
                if (entry.getValue() == manager) {
                    it.remove();
                    if (outstandingManagers.isEmpty()) {
                        finishSuspend();
                        AS_ROOT_LOGGER.serverSuspended();
                    }
                }
            }
        }
    }

    /**
     * Complete the shutdown process
     */
    private void finishSuspend() {
        //notify the listeners
        for (final SuspendListener listener : listeners) {
            try {
                listener.suspendComplete();
            } catch (Exception e) {
                AS_ROOT_LOGGER.suspendListenerThrewException(e, listener.toString(), "suspendComplete");
            }
        }
        suspendState = SuspendState.SUSPENDED;
        outstandingManagers = null;
    }

    private void threadManagerShutdown(final ShutdownCompleteCallback callback) {
        synchronized (this) {
            if (outstandingManagers == null) {
                //the shutdown has been cancelled
                return;
            }
            outstandingManagers.remove(callback);
            if (outstandingManagers.isEmpty()) {
                finishSuspend();
                AS_ROOT_LOGGER.serverSuspended();
            }
        }
    }

    public SuspendState getSuspendState() {
        return suspendState;
    }

    /**
     * Listener that thread managers can use to notify the shutdown manager that
     * they have shut down successfully.
     */
    public class ShutdownCompleteCallback {

        public void shutdownComplete() {
            threadManagerShutdown(this);
        }
    }


    /**
     * Listener that allows code to await suspension completion
     */
    private final class ShutdownServiceListener implements SuspendListener {

        private boolean finished = false;
        boolean completed = false;

        @Override
        public boolean listenerAdded(final SuspendState suspendState) {
            synchronized (this) {
                //if we have already suspended
                if (suspendState == SuspendState.SUSPENDED) {
                    finished = true;
                    completed = true;
                    notifyAll();
                    return false;
                }
                //if suspension has been cancelled
                if (suspendState == SuspendState.RUNNING) {
                    finished = true;
                    completed = false;
                    notifyAll();
                    return false;
                }
                return true;
            }
        }

        @Override
        public void suspendStarted() {

        }

        @Override
        public void suspendComplete() {
            synchronized (this) {
                completed = true;
                finished = true;
                notifyAll();
            }
        }


        @Override
        public void resumed() {
            synchronized (this) {
                completed = false;
                finished = true;
                notifyAll();
            }
        }

        public boolean waitTillFinished() {
            synchronized (this) {
                if (!finished) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return completed;
        }

        public void waitTillFinished(long waitTime) {
            synchronized (this) {
                if (!finished) {
                    try {
                        wait(waitTime);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
