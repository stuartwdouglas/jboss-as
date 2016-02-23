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
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import javax.ejb.Startup;
import java.util.Collection;

/**
 * Handles {@link Startup}
 * @author Stuart Douglas
 */
public class StartupMergingProcessor extends AbstractMergingProcessor<SingletonComponentDescription> {

    private static final ServiceName SINGLETON_STARTUP_SERVICE_NAME = ServiceName.of("ejb", "singleton-startup-dependency");

    public StartupMergingProcessor() {
        super(SingletonComponentDescription.class);
    }

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        super.deploy(phaseContext);
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Collection<ComponentDescription> componentConfigurations = eeModuleDescription.getComponentDescriptions();

        ServiceName serviceName = deploymentUnit.getServiceName().append(SINGLETON_STARTUP_SERVICE_NAME);
        ServiceBuilder<Void> builder = phaseContext.getServiceTarget().addService(serviceName, Service.NULL);

        //now setup the dependencies, according to the spec we can't start any EJB's until all @Startup singletons
        //have started
        boolean required = false;
        for (ComponentDescription description : componentConfigurations) {
            if (EJBComponentDescription.class.isAssignableFrom(description.getClass())) {
                if(SingletonComponentDescription.class.isAssignableFrom(description.getClass())) {
                    SingletonComponentDescription singletonComponentDescription = (SingletonComponentDescription) description;
                    if(singletonComponentDescription.isInitOnStartup()) {
                        required = true;
                        builder.addDependency(singletonComponentDescription.getStartServiceName());
                    }
                }
            }
        }
        //for consistencies sake we always install the service, even if it is not strictly required
        builder.install();
        if(required) {
            for (ComponentDescription description : componentConfigurations) {
                if (EJBComponentDescription.class.isAssignableFrom(description.getClass())) {
                    if(SingletonComponentDescription.class.isAssignableFrom(description.getClass())) {
                        SingletonComponentDescription singletonComponentDescription = (SingletonComponentDescription) description;
                        if(singletonComponentDescription.isInitOnStartup()) {
                            break;
                        }
                    }
                    description.getConfigurators().add((context, desc, configuration) -> configuration.getStartDependencies().add((b, service) -> b.addDependency(serviceName)));
                }
            }
        }
    }


    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SingletonComponentDescription description) throws DeploymentUnitProcessingException {
        EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        if (clazz != null) {
            final ClassAnnotationInformation<Startup, Object> data = clazz.getAnnotationInformation(Startup.class);
            if (data != null) {
                if (!data.getClassLevelAnnotations().isEmpty()) {
                    description.initOnStartup();
                }
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final SingletonComponentDescription description) throws DeploymentUnitProcessingException {
        SessionBeanMetaData data = description.getDescriptorData();
        if (data instanceof SessionBean31MetaData) {
            SessionBean31MetaData singletonBeanMetaData = (SessionBean31MetaData) data;
            Boolean initOnStartup = singletonBeanMetaData.isInitOnStartup();
            if (initOnStartup != null && initOnStartup) {
                description.initOnStartup();
            }
        }
    }
}
