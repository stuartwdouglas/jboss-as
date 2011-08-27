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

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Interceptor that hooks up finder methods for BMP entity beans
 * <p/>
 * This is a view level interceptor that should be attached to finder methods on the home interface.
 *
 * @author Stuart Douglas
 */
public class EntityBeanHomeFinderInterceptorFactory implements InterceptorFactory {

    private final Method finderMethod;
    private final InjectedValue<ComponentView> viewToCreate = new InjectedValue<ComponentView>();

    public EntityBeanHomeFinderInterceptorFactory(final Method finderMethod) {
        this.finderMethod = finderMethod;
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {

        final EntityBeanComponent component = (EntityBeanComponent) context.getContextData().get(Component.class);

        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {

                //grab a bean from the pool to invoke the finder method on
                final EntityBeanComponentInstance instance = component.getPool().get();
                final Object result;
                try {
                    //forward the invocation to the component interceptor chain
                    Method oldMethod = context.getMethod();
                    try {
                        context.putPrivateData(ComponentInstance.class, instance);
                        context.setMethod(finderMethod);
                        context.setTarget(instance.getInstance());
                        result = instance.getInterceptor(finderMethod).processInvocation(context);
                    } finally {
                        context.setMethod(oldMethod);
                        context.setTarget(null);
                        context.putPrivateData(ComponentInstance.class, null);
                    }
                    if (result instanceof Collection) {
                        Collection keys = (Collection) result;
                        final Set<Object> results = new HashSet<Object>();
                        for (Object key : keys) {
                            results.add(getLocalObject(key, component));
                        }
                        return results;
                    } else {
                        return getLocalObject(result, component);
                    }

                } finally {
                    component.getPool().release(instance);
                }
            }

        };
    }

    private Object getLocalObject(final Object result, final EntityBeanComponent component) {
        final EntityBeanComponentInstance res = component.getCache().get(result);
        final HashMap<Object, Object> create = new HashMap<Object, Object>();
        create.put(ComponentInstance.class, res);
        return  viewToCreate.getValue().createInstance(create).createProxy();
    }

    public InjectedValue<ComponentView> getViewToCreate() {
        return viewToCreate;
    }
}
