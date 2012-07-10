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

package org.jboss.as.web.deployment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.catalina.core.StandardContext;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;

import static org.jboss.as.web.WebLogger.WEB_LOGGER;
import static org.jboss.as.web.WebMessages.MESSAGES;

/**
 * Provides an API to start/stop the {@link org.jboss.as.web.deployment.WebDeploymentService}.
 * This should register/deregister the web context.
 */
public class ContextActivator {

    public static final AttachmentKey<ContextActivator> ATTACHMENT_KEY = AttachmentKey.create(ContextActivator.class);

    private final ServiceController<StandardContext> controller;

    ContextActivator(ServiceController<StandardContext> controller) {
        this.controller = controller;
    }

    /**
     * Start the web context asynchronously.
     * This would happen during normal WAR deployment
     */
    public synchronized void startAsync() {
        controller.setMode(ServiceController.Mode.ACTIVE);
    }

    /**
     * Start the web context synchronously.
     * This would happen when an OSGi Web Application Bundle (WAB) transitions to {@link Bundle#ACTIVE}
     * i.e. the WAB starts
     */
    public synchronized boolean start(long timeout, TimeUnit unit) throws TimeoutException {
        boolean result = true;
        if (controller.getMode() == ServiceController.Mode.NEVER) {
            controller.setMode(ServiceController.Mode.ACTIVE);
            result = awaitStateChange(ServiceController.State.UP, timeout, unit);
        }
        return result;
    }

    /**
     * Stop the web context synchronously.
     * This would happen when an OSGi Web Application Bundle (WAB) transitions to {@link Bundle#RESOLVED}
     * i.e. the WAB stops
     */
    public synchronized boolean stop(long timeout, TimeUnit unit) {
        boolean result = true;
        if (controller.getMode() == ServiceController.Mode.ACTIVE) {
            controller.setMode(ServiceController.Mode.NEVER);
            try {
                result = awaitStateChange(ServiceController.State.DOWN, timeout, unit);
            } catch (TimeoutException ex) {
                WEB_LOGGER.debugf("Timeout stopping context: %s", controller.getName());
            }
        }
        return result;
    }

    private boolean awaitStateChange(final ServiceController.State expectedState, long timeout, TimeUnit unit) throws TimeoutException {
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceListener<StandardContext> listener = new AbstractServiceListener<StandardContext>() {

            @Override
            public void listenerAdded(ServiceController<? extends StandardContext> controller) {
                ServiceController.State state = controller.getState();
                if (state == expectedState || state == ServiceController.State.START_FAILED)
                    listenerDone(controller);
            }

            @Override
            public void transition(final ServiceController<? extends StandardContext> controller, final ServiceController.Transition transition) {
                if (expectedState == ServiceController.State.UP) {
                    switch (transition) {
                        case STARTING_to_UP:
                        case STARTING_to_START_FAILED:
                            listenerDone(controller);
                            break;
                    }
                } else if (expectedState == ServiceController.State.DOWN) {
                    switch (transition) {
                        case STOPPING_to_DOWN:
                        case REMOVING_to_DOWN:
                        case WAITING_to_DOWN:
                            listenerDone(controller);
                            break;
                    }
                }
            }

            private void listenerDone(ServiceController<? extends StandardContext> controller) {
                latch.countDown();
            }
        };

        controller.addListener(listener);
        try {
            if (latch.await(timeout, unit) == false) {
                throw MESSAGES.timeoutContextActivation(controller.getName());
            }
        } catch (InterruptedException e) {
            // ignore
        } finally {
            controller.removeListener(listener);
        }

        return controller.getState() == expectedState;
    }
}
