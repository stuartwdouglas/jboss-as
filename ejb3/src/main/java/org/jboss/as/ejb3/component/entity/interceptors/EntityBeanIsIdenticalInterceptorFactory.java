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
package org.jboss.as.ejb3.component.entity.interceptors;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.ejb.client.EntityEJBLocator;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * Interceptor that handles the {@link javax.ejb.EJBLocalObject#isIdentical(javax.ejb.EJBLocalObject)}
 * && {@link javax.ejb.EJBObject#isIdentical(javax.ejb.EJBObject)} methods
 *
 * @author Stuart Douglas
 */
public class EntityBeanIsIdenticalInterceptorFactory implements InterceptorFactory {

    public static final EntityBeanIsIdenticalInterceptorFactory INSTANCE = new EntityBeanIsIdenticalInterceptorFactory();

    private EntityBeanIsIdenticalInterceptorFactory() {
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {
        final ComponentView componentView = (ComponentView) context.getContextData().get(ComponentView.class);
        return new EntityIsIdenticalInterceptor(componentView);

    }

    private class EntityIsIdenticalInterceptor implements Interceptor {

        private final ComponentView componentView;

        public EntityIsIdenticalInterceptor(final ComponentView componentView) {
            this.componentView = componentView;
        }

        @Override
        public Object processInvocation(final InterceptorContext context) throws Exception {
            final Object primaryKey = context.getPrivateData(EntityBeanComponent.PRIMARY_KEY_CONTEXT_KEY);
            final Object other = context.getParameters()[0];
            final Class<?> proxyType = componentView.getProxyClass();
            if (proxyType.isAssignableFrom(other.getClass())) {
                //now we know that this is an ejb for the correct component view
                //as digging out the session id from the proxy object is not really
                //a viable option, we invoke equals() for the other instance with a
                //PrimaryKeyHolder as the other side
                if (other instanceof EJBLocalObject) {
                    return ((EJBLocalObject) other).isIdentical(new PrimaryKeyHolder(primaryKey));
                } else if (other instanceof EJBObject) {
                    return ((EJBObject) other).isIdentical(new PrimaryKeyHolder(primaryKey));
                } else {
                    throw new RuntimeException(getClass() + " was attached to a view that is not an EJBObject or a EJBLocalObject");
                }
            } else if (other instanceof PrimaryKeyHolder) {
                return primaryKey.equals(((PrimaryKeyHolder) other).primaryKey);
            } else {
                return false;
            }
        }
    }


    private static class PrimaryKeyHolder implements EJBLocalObject, EJBObject {
        private final Object primaryKey;

        public PrimaryKeyHolder(final Object primaryKey) {
            this.primaryKey = primaryKey;
        }

        @Override
        public EJBLocalHome getEJBLocalHome() throws EJBException {
            return null;
        }

        @Override
        public EJBHome getEJBHome() throws RemoteException {
            return null;
        }

        @Override
        public Object getPrimaryKey() throws EJBException {
            return null;
        }

        @Override
        public void remove() throws RemoveException, EJBException {

        }

        @Override
        public Handle getHandle() throws RemoteException {
            return null;
        }

        @Override
        public boolean isIdentical(final EJBObject ejbo) throws RemoteException {
            return false;
        }

        @Override
        public boolean isIdentical(final EJBLocalObject obj) throws EJBException {
            return false;
        }
    }

}
