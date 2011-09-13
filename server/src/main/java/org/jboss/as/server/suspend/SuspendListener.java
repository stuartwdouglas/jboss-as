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

/**
 * Listener that can be notified of shutdown events.
 *
 * @author Stuart Douglas
 */
public interface SuspendListener {

    /**
     * Called when a listener is added
     * @param suspendState The current suspend state
     * @return true if the listener should be added
     */
    boolean listenerAdded(final SuspendState suspendState);

    /**
     * Called when a deployment starts to shut down
     */
    void suspendStarted();

    /**
     * Called after a suspend is complete. This may not be called in every case,
     * if the deployment is resumed before the suspend is completed, or if the app server
     * is shut down before the suspend operation is complete.
     *
     */
    void suspendComplete();

    /**
     * Called when the deployment is resumed
     *
     */
    void resumed();

}
