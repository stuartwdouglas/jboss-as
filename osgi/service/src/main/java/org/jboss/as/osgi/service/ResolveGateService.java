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

package org.jboss.as.osgi.service;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that installs a 'gate' service when this service starts. This effectivly creates a weak service
 * dependency, as a phase that depends on the installed service will not start until this service starts,
 * however removal of this service will not roll back the deployment.
 *
 *
 * @author Stuart Douglas
 */
public class ResolveGateService implements Service<ResolveGateService> {

    public static final ServiceName GATE_SERVICE_NAME = ServiceName.of("osgi", "gateService");

    public static final ServiceName GATE_OPENER_SERVICE = ServiceName.of("osgi", "gateOpenerService");

    private final ServiceTarget serviceTarget;
    final ServiceName serviceName;

    public ResolveGateService(final ServiceTarget serviceTarget, final ServiceName serviceName) {
        this.serviceTarget = serviceTarget;
        this.serviceName = serviceName;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        serviceTarget.addService(serviceName.append(GATE_SERVICE_NAME), Service.NULL)
                .install();
    }

    @Override
    public void stop(final StopContext stopContext) {

    }

    @Override
    public ResolveGateService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public static ServiceName gateService(final DeploymentUnit deploymentUnit) {
        final ServiceName top = deploymentUnit.getParent() == null ? deploymentUnit.getServiceName() : deploymentUnit.getParent().getServiceName();
        return top.append(GATE_SERVICE_NAME);
    }

}
