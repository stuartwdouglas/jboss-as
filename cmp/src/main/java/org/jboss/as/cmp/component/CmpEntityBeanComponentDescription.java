/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cmp.component;

import org.jboss.as.cmp.component.interceptors.CmpEntityBeanSynchronizationInterceptor;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstanceInterceptorFactory;
import org.jboss.as.ee.component.ComponentStartService;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEApplicationDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentDescription;
import org.jboss.as.ejb3.component.entity.EntityBeanHomeViewConfigurator;
import org.jboss.as.ejb3.component.entity.EntityBeanObjectViewConfigurator;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class CmpEntityBeanComponentDescription extends EntityBeanComponentDescription {
    private JDBCEntityMetaData entityMetaData;

    public CmpEntityBeanComponentDescription(String componentName, String componentClassName, EjbJarDescription ejbJarDescription, ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, ejbJarDescription, deploymentUnitServiceName);

        getConfigurators().addFirst(new ComponentConfigurator() {
            public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                final CmpInstanceReferenceFactory factory = new CmpInstanceReferenceFactory(configuration.getComponentClass(), ((CmpEntityBeanComponentDescription)description).getEntityMetaData().getLocalHomeClass(), ((CmpEntityBeanComponentDescription)description).getEntityMetaData().getHomeClass());
                configuration.getStartDependencies().add(new DependencyConfigurator<ComponentStartService>() {
                    public void configureDependency(final ServiceBuilder<?> serviceBuilder, final ComponentStartService service) throws DeploymentUnitProcessingException {
                        serviceBuilder.addDependency(description.getCreateServiceName(), CmpEntityBeanComponent.class, factory.getComponentInjector());
                    }
                });
                configuration.setInstanceFactory(factory);
            }
        });
    }

    @Override
    public ComponentConfiguration createConfiguration(EEApplicationDescription applicationDescription) {
        final ComponentConfiguration configuration = new ComponentConfiguration(this, applicationDescription.getClassConfiguration(getComponentClassName()));
        configuration.setComponentCreateServiceFactory(CmpEntityBeanComponentCreateService.FACTORY);
        return configuration;
    }

    protected EntityBeanObjectViewConfigurator getObjectViewConfigurator() {
        return new CmpEntityBeanObjectViewConfigurator();
    }

    protected EntityBeanHomeViewConfigurator getHomeViewConfigurator() {
        return new CmpEntityBeanHomeViewConfigurator();
    }

    protected void addSynchronizationInterceptor() {
        // we must run before the DefaultFirstConfigurator
        getConfigurators().addFirst(new ComponentConfigurator() {
            public void configure(DeploymentPhaseContext context, ComponentDescription description, ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                final InterceptorFactory interceptorFactory = new ComponentInstanceInterceptorFactory() {
                    protected Interceptor create(Component component, InterceptorFactoryContext context) {
                        return new CmpEntityBeanSynchronizationInterceptor(isReentrant());
                    }
                };
                configuration.addComponentInterceptor(interceptorFactory, InterceptorOrder.Component.SYNCHRONIZATION_INTERCEPTOR, false);
            }
        });
    }

    public JDBCEntityMetaData getEntityMetaData() {
        return entityMetaData;
    }

    public void setEntityMetaData(JDBCEntityMetaData entityMetaData) {
        this.entityMetaData = entityMetaData;
    }
}
