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

package org.jboss.as.cmp.component.interceptors;

import java.lang.reflect.Method;
import org.jboss.as.cmp.component.CmpEntityBeanComponentInstance;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.as.ejb3.component.entity.interceptors.EntityBeanHomeFinderInterceptorFactory;
import org.jboss.invocation.InterceptorContext;

/**
 * @author John Bailey
 */
public class CmpEntityBeanHomeFinderInterceptorFactory extends EntityBeanHomeFinderInterceptorFactory {
    private final Method finderMethod;

    public CmpEntityBeanHomeFinderInterceptorFactory(final Method finderMethod) {
        super(finderMethod);
        this.finderMethod = finderMethod;
    }


    protected Object invokeFind(final InterceptorContext context, final EntityBeanComponentInstance instance) throws Exception {
        try {
            instance.getComponent().getComponentClass().getDeclaredMethod(finderMethod.getName(), finderMethod.getParameterTypes());
            return super.invokeFind(context, instance);
        } catch (NoSuchMethodException ignored) {}

        final CmpEntityBeanComponentInstance cmpInstance = CmpEntityBeanComponentInstance.class.cast(instance);
        final JDBCEntityPersistenceStore store = cmpInstance.getComponent().getStoreManager();

        if (getReturnType() == ReturnType.SINGLE) {
            return store.findEntity(context.getMethod(), context.getParameters(), cmpInstance.getEntityContext());
        } else {
            return store.findEntities(context.getMethod(), context.getParameters(), cmpInstance.getEntityContext());
        }
    }
}
