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
package org.jboss.as.naming.deployment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Adds a service that depends on all JNDI bindings from the deployment to be up.
 * <p/>
 * As binding services are not children of the root deployment unit service this service
 * is necessary to ensure the deployment is not considered complete until add bindings are up
 *
 * @author Stuart Douglas
 */
public class JndiNamingDependencyProcessor implements DeploymentUnitProcessor {

    private static final ServiceName JNDI_DEPENDENCY_SERVICE = ServiceName.of("jndiDependencyService");


    private static final ServiceName JNDI_DEPENDENCY_INDIRECTION_SERVICE = ServiceName.of("jndiDependencyIndirectionService");

    /**
     * MSC has a limit of at most 16383 dependencies. In some situations we can have more that this, so we create an intermediate dependency service
     * for every 10000 dependencies.
     *
     * This situation mostly happens when there is a very large number of EJB's in a deployment.
     */
    private static final int MAX_DEPENDENCIES = 10000;

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        ServiceName namingStoreServiceName = support.getCapabilityServiceName(NamingService.CAPABILITY_NAME);
        //this will always be up but we need to make sure the naming service is
        //not shut down before the deployment is undeployed when the container is shut down
        phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, namingStoreServiceName);

        Set<ServiceName> dependencies = new HashSet<>(deploymentUnit.getAttachmentList(Attachments.JNDI_DEPENDENCIES));
        if(deploymentUnit.getParent() != null) {
            dependencies.addAll(deploymentUnit.getParent().getAttachment(Attachments.JNDI_DEPENDENCIES));
        }
        final ServiceName serviceName = serviceName(deploymentUnit.getServiceName());
        final ServiceBuilder<?> serviceBuilder = phaseContext.getServiceTarget().addService(serviceName, new RuntimeBindReleaseService());
        List<ServiceName> indirectDependencies = new ArrayList<>();
        int i = 0, count = 0;
        ServiceName indirect = null;
        ServiceBuilder<Void> indirectBuilder = null;
        for(ServiceName dep : dependencies) {
            if(i % MAX_DEPENDENCIES == 0) {
                if(indirectBuilder != null) {
                    indirectBuilder.install();
                }
                indirect = serviceName.append(JNDI_DEPENDENCY_INDIRECTION_SERVICE).append(Integer.toString(count++));
                indirectBuilder = phaseContext.getServiceTarget().addService(indirect, Service.NULL);
                indirectDependencies.add(indirect);
            }
            indirectBuilder.addDependency(dep);
            ++i;
        }
        if(indirectBuilder != null) {
            indirectBuilder.install();
        }
        serviceBuilder.addDependencies(indirectDependencies);

        serviceBuilder.addDependency(namingStoreServiceName);
        serviceBuilder.install();
    }

    public static ServiceName serviceName(final ServiceName deploymentUnitServiceName) {
        return deploymentUnitServiceName.append(JNDI_DEPENDENCY_SERVICE);
    }

    public static ServiceName serviceName(final DeploymentUnit deploymentUnit) {
        return serviceName(deploymentUnit.getServiceName());
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.JNDI_DEPENDENCIES);
    }

}
