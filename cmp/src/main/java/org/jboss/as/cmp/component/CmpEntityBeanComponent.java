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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.transaction.Transaction;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.bridge.CMRInvocation;
import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.naming.ManagedReference;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.Value;

/**
 * @author John Bailey
 */
public class CmpEntityBeanComponent extends EntityBeanComponent {

    private final Value<JDBCEntityPersistenceStore> storeManager;
    private final Class<?> homeClass;
    private final Class<?> localHomeClass;

    public CmpEntityBeanComponent(final CmpEntityBeanComponentCreateService ejbComponentCreateService, final Value<JDBCEntityPersistenceStore> storeManager) {
        super(ejbComponentCreateService);

        this.homeClass = ejbComponentCreateService.getHomeClass();
        this.localHomeClass = ejbComponentCreateService.getLocalHomeClass();

        this.storeManager = storeManager;
    }

    protected BasicComponentInstance instantiateComponentInstance(final AtomicReference<ManagedReference> instanceReference, final Interceptor preDestroyInterceptor, final Map<Method, Interceptor> methodInterceptors, final InterceptorFactoryContext interceptorContext) {
        return new CmpEntityBeanComponentInstance(this, instanceReference, preDestroyInterceptor, methodInterceptors);
    }

    public Class<?> getHomeClass() {
        return homeClass;
    }

    public Class<?> getLocalHomeClass() {
        return localHomeClass;
    }

    public void start() {
        super.start();
        if (storeManager == null || storeManager.getValue() == null) {
            throw new IllegalStateException("Store manager not set");
        }
    }

    public Collection<Object> getEntityLocalCollection(List<Object> idList) {
        return null;  // TODO: jeb - This should return proxy instances to local entities
    }

    public EJBLocalObject getEntityEJBLocalObject(Object currentId) {
        return null;  // TODO: jeb - this should return a proxy instance to local a entity
    }

    public EJBObject getEntityEJBObject(Object id) {
        return null;  // TODO: jeb - this should return a proxy instance to remote a entity
    }

    public Object invoke(CMRInvocation invocation) {
        return null;  // TODO: jeb - run the invocation against an instance
    }

    public static void synchronizeEntitiesWithinTransaction(Transaction transaction) {
        // TODO: jeb - run sync of all the entities in the transaction
    }

    public EJBLocalHome getEJBLocalHome() throws IllegalStateException {
        return (EJBLocalHome) createViewInstanceProxy(getLocalHomeClass(), Collections.emptyMap());
    }

    public JDBCEntityPersistenceStore getStoreManager() {
        return storeManager.getValue();
    }
}
