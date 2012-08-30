/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.jsf.injection;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.myfaces.config.annotation.LifecycleProvider2;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.web.common.StartupContext;
import org.jboss.as.web.common.util.ConcurrentReferenceHashMap;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class MyFacesLifecycleProvider implements LifecycleProvider2 {

    private final ComponentRegistry componentRegistry;
    private final Map<Object, ManagedReference> instanceMap;

    public MyFacesLifecycleProvider() {
        this.componentRegistry = StartupContext.getComponentRegistry();
        this.instanceMap = new ConcurrentReferenceHashMap<Object, ManagedReference>
                (256, ConcurrentReferenceHashMap.DEFAULT_LOAD_FACTOR,
                        Runtime.getRuntime().availableProcessors(), ConcurrentReferenceHashMap.ReferenceType.STRONG,
                        ConcurrentReferenceHashMap.ReferenceType.STRONG, EnumSet.of(ConcurrentReferenceHashMap.Option.IDENTITY_COMPARISONS));
    }

    public Object newInstance(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NamingException, InvocationTargetException {
        ManagedReference instance =  componentRegistry.createInstance(Thread.currentThread().getContextClassLoader().loadClass(className));
        instanceMap.put(instance.getInstance(), instance);
        return instance.getInstance();
    }

    public void postConstruct(Object obj) throws IllegalAccessException, InvocationTargetException {
        // do nothing.  container.newInstance() took care of this.
    }

    public void destroyInstance(Object obj) throws IllegalAccessException, InvocationTargetException {
        ManagedReference instance = instanceMap.get(obj);
        if(instance != null) {
            instance.release();
        }
    }

}
