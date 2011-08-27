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
package org.jboss.as.ejb3.component.entity;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

import java.lang.reflect.Method;

/**
 * configurator that sets up interceptors for an EJB's object view
 *
 * @author Stuart Douglas
 */
public class EntityBeanObjectViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {

        final Object primaryKeyContextKey = new Object();

        configuration.addViewPostConstructInterceptor(new EntityBeanEjbCreateMethodInterceptorFactory(primaryKeyContextKey), InterceptorOrder.ViewPostConstruct.INSTANCE_CREATE);
        configuration.addViewInterceptor(new EntityBeanAssociatingInterceptorFactory(primaryKeyContextKey), InterceptorOrder.View.ASSOCIATING_INTERCEPTOR);


        for (final Method method : configuration.getProxyFactory().getCachedMethods()) {

            if (method.getName().equals("getPrimaryKey") && method.getParameterTypes().length == 0) {
                configuration.addClientInterceptor(method, ViewDescription.CLIENT_DISPATCHER_INTERCEPTOR_FACTORY, InterceptorOrder.Client.CLIENT_DISPATCHER);
                configuration.addViewInterceptor(method, EntityBeanInterceptors.GET_PRIMARY_KEY, InterceptorOrder.View.COMPONENT_DISPATCHER);
            } else {

            }
        }

    }

}
