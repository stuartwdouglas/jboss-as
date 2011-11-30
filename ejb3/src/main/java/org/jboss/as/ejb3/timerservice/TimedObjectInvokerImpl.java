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
package org.jboss.as.ejb3.timerservice;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.TimerInvocationMarker;
import org.jboss.as.ee.utils.DescriptorUtils;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.invocation.InterceptorContext;

/**
 * Timed object invoker for singleton EJB's
 *
 * @author Stuart Douglas
 */
public class TimedObjectInvokerImpl implements TimedObjectInvoker, Serializable {

    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    private final EJBComponent ejbComponent;

    /**
     * String that uniquely identifies a deployment
     */
    private final String deploymentString;

    public TimedObjectInvokerImpl(final EJBComponent ejbComponent, final String deploymentString) {
        this.ejbComponent = ejbComponent;
        this.deploymentString = deploymentString;
    }

    @Override
    public void callTimeout(final TimerImpl timer, final Method timeoutMethod) throws Exception {

        final ComponentView view = ejbComponent.getTimerView();
        Method viewMethod = view.getMethod(timeoutMethod.getName(), DescriptorUtils.methodDescriptor(timeoutMethod));
        try {
            final InterceptorContext context = new InterceptorContext();
            context.putPrivateData(Component.class, ejbComponent);
            context.putPrivateData(ComponentView.class, view);
            context.setContextData(new HashMap<String, Object>());
            context.putPrivateData(MethodIntf.class, MethodIntf.TIMER);
            context.setMethod(viewMethod);
            context.setTimer(timer);
            context.putPrivateData(TimerInvocationMarker.class, TimerInvocationMarker.INSTANCE);
            context.putPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY, timer.getPrimaryKey());

            final Object[] params;
            if (timeoutMethod.getParameterTypes().length == 1) {
                params = new Object[1];
                params[0] = timer;
            } else {
                params = EMPTY_OBJECT_ARRAY;
            }
            context.setParameters(params);
            view.invoke(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTimedObjectId() {
        return deploymentString + "." + ejbComponent.getComponentName();
    }

    @Override
    public void callTimeout(final TimerImpl timer) throws Exception {
        callTimeout(timer, ejbComponent.getTimeoutMethod());
    }

    public ClassLoader getClassLoader() {
        return ejbComponent.getComponentClass().getClassLoader();
    }
}
