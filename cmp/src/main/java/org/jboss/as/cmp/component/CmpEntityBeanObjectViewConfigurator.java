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
import org.jboss.as.cmp.component.interceptors.CmpEntityBeanEjbCreateMethodInterceptorFactory;
import org.jboss.as.cmp.component.interceptors.CmpEntityBeanRemoveInterceptorFactory;
import org.jboss.as.ejb3.component.entity.EntityBeanObjectViewConfigurator;
import org.jboss.invocation.InterceptorFactory;

/**
 * @author John Bailey
 */
public class CmpEntityBeanObjectViewConfigurator extends EntityBeanObjectViewConfigurator {
    protected InterceptorFactory getEjbCreateInterceptorFactory(Object primaryKeyContextKey) {
        return new CmpEntityBeanEjbCreateMethodInterceptorFactory(primaryKeyContextKey);
    }

    protected InterceptorFactory getEjbRemoveInterceptorFactory(Method remove, Object primaryKeyContextKey) {
        return new CmpEntityBeanRemoveInterceptorFactory(remove, primaryKeyContextKey);
    }
}
