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
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.classfilewriter.ClassMethod;
import org.jboss.invocation.proxy.MethodBodyCreator;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;

/**
 * @author John Bailey
 */
public class CmpProxyFactory<T> extends ProxyFactory<T> {
    private static final AtomicInteger PROXY_ID = new AtomicInteger(0);

    public static <T> ProxyFactory<T> createProxyFactory(final Class<T> beanClass, final Class<?>[] homeClasses) {
        final ProxyConfiguration<T> proxyConfiguration = new ProxyConfiguration<T>();
        proxyConfiguration.setProxyName(beanClass.getName() + "$$$cmp" + PROXY_ID.incrementAndGet());
        proxyConfiguration.setClassLoader(beanClass.getClassLoader());
        proxyConfiguration.setProtectionDomain(beanClass.getProtectionDomain());
        proxyConfiguration.setSuperClass(beanClass);
        if (homeClasses != null) for (Class<?> homeClass : homeClasses) {
            if (homeClass != null) {
                proxyConfiguration.addAdditionalInterface(homeClass);
            }
        }
        proxyConfiguration.addAdditionalInterface(CmpProxy.class);
        return new CmpProxyFactory<T>(proxyConfiguration);
    }

    public CmpProxyFactory(final ProxyConfiguration<T> proxyConfiguration) {
        super(proxyConfiguration);
    }

    protected boolean overrideMethod(final Method method, final MethodIdentifier identifier, final MethodBodyCreator creator) {
        return Modifier.isAbstract(method.getModifiers()) && super.overrideMethod(method, identifier, creator);
    }

    protected boolean overrideMethod(final ClassMethod method, MethodIdentifier identifier, MethodBodyCreator creator) {
        return Modifier.isAbstract(method.getAccessFlags()) && super.overrideMethod(method, identifier, creator);
    }
}
