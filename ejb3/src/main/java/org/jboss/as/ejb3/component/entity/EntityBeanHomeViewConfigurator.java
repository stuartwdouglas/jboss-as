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
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

import java.lang.reflect.Method;

/**
 * View configurator for the home interface of an entity bean
 *
 * @author Stuart Douglas
 */
public class EntityBeanHomeViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration, final ViewDescription description, final ViewConfiguration configuration) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = context.getDeploymentUnit();
        final DeploymentReflectionIndex deploymentReflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);

        configuration.addClientPostConstructInterceptor(org.jboss.invocation.Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPostConstruct.TERMINAL_INTERCEPTOR);
        configuration.addClientPreDestroyInterceptor(org.jboss.invocation.Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ClientPreDestroy.TERMINAL_INTERCEPTOR);

        configuration.addViewPostConstructInterceptor(org.jboss.invocation.Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ViewPostConstruct.TERMINAL_INTERCEPTOR);
        configuration.addViewPreDestroyInterceptor(org.jboss.invocation.Interceptors.getTerminalInterceptorFactory(), InterceptorOrder.ViewPreDestroy.TERMINAL_INTERCEPTOR);

        for (final Method method : configuration.getProxyFactory().getCachedMethods()) {
            if (method.getName().equals("equals") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class) {
                //TODO:
            } else if (method.getName().equals("hashCode") && method.getParameterTypes().length == 0) {
                //TODO
            } else if (method.getName().startsWith("create")) {
                //we have a create method.
                //lets resolve the corresponding ejbCreate method
                final Method ejbCreate = resolveCreateMethod("ejbCreate",componentConfiguration.getComponentClass(), deploymentReflectionIndex, method, componentConfiguration.getComponentName());
                final Method ejbPostCreate = resolveCreateMethod("ejbPostCreate",componentConfiguration.getComponentClass(), deploymentReflectionIndex, method, componentConfiguration.getComponentName());

                configuration.addViewInterceptor(method, new EntityBeanHomeCreateInterceptorFactory(ejbCreate, ejbPostCreate), InterceptorOrder.View.HOME_CREATE_INTERCEPTOR);
                
            } else if(method.getName().startsWith("find")) {

            } else if(method.getName().equals("remove")) {

            } else {

            }
        }
    }


    private Method resolveCreateMethod(final  String prefix, final Class<?> componentClass, final DeploymentReflectionIndex index, final Method method, final String ejbName) throws DeploymentUnitProcessingException {

        final String name = method.getName().replaceFirst("create", prefix);
        Class<?> clazz = componentClass;
        while (clazz != Object.class) {
            final ClassReflectionIndex<?> classIndex = index.getClassIndex(clazz);
            Method ret = classIndex.getMethod(method.getReturnType(), name, method.getParameterTypes());
            if (ret != null) {
                return ret;
            }
            clazz = clazz.getSuperclass();
        }
        throw new DeploymentUnitProcessingException("Could not resolve corresponding ejbCreate for home interface method " + method + " on EJB " + ejbName);
    }
}
