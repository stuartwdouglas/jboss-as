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
package org.jboss.as.web.deployment;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.as.server.suspend.ServerSuspendingException;
import org.jboss.as.server.suspend.SuspendPermit;

/**
 * {@link Valve} responsible for handling the shutdown of web worker threads
 *
 * @author Stuart Douglas
 */
public class SuspendValve extends ValveBase {

    private final WebSuspendManagerService suspendPermitManager;

    public SuspendValve(final WebSuspendManagerService suspendPermitManager) {
        this.suspendPermitManager = suspendPermitManager;
    }


    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {
        SuspendPermit permit = null;
        try {
            permit = suspendPermitManager.getSuspendPermitManager().acquirePermit();
            getNext().invoke(request, response);
        } catch (ServerSuspendingException e) {
            //just set a 503 (Temporarily unavailable) code and return
            response.setStatus(503);
        } finally {
            if (permit != null) {
                suspendPermitManager.getSuspendPermitManager().releasePermit(permit);
            }
        }
    }

}
