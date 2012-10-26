/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

/**
 * Registry that can be used to directly look up a component, based on the component class.
 * <p/>
 * This is obviously not ideal, as it is possible to have multiple components for a single class,
 * however it is necessary to work around problematic SPI's that expect to be able to inject / instantiate
 * based only on the class type.
 * <p/>
 * This registry only contains simple component types that do not have a view
 *
 * @author Stuart Douglas
 */
public class ComponentRegistry implements Service<ComponentRegistry> {

    public static ServiceName SERVICE_NAME = ServiceName.of("ee", "ComponentRegistry");

    private final Map<Class<?>, ServiceName> componentsByClass = new ConcurrentHashMap<Class<?>, ServiceName>();
    private final ServiceRegistry serviceRegistry;

    public ComponentRegistry(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void addComponent(final ComponentConfiguration componentConfiguration) {
        componentsByClass.put(componentConfiguration.getComponentClass(), componentConfiguration.getComponentDescription().getStartServiceName());
    }

    public ManagedReference createInstance(final Class<?> componentClass) {
        final ServiceName name = componentsByClass.get(componentClass);
        if (name == null) {
            return null;
        }
        ServiceController<Component> component = (ServiceController<Component>) serviceRegistry.getService(name);
        if (component == null) {
            return null;
        }
        return new ComponentManagedReference(component.getValue().createInstance());
    }

    public ManagedReference createInstance(final Object instance) {
        final ServiceName name = componentsByClass.get(instance.getClass());
        if (name == null) {
            return null;
        }
        ServiceController<Component> component = (ServiceController<Component>) serviceRegistry.getService(name);
        if (component == null) {
            return null;
        }
        return new ComponentManagedReference(component.getValue().createInstance(instance));
    }

    @Override
    public void start(final StartContext startContext) throws StartException {

    }

    @Override
    public void stop(final StopContext stopContext) {
    }

    @Override
    public ComponentRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Collection<ServiceName> serviceNames() {
        return componentsByClass.values();
    }

    private static class ComponentManagedReference implements ManagedReference {

        private final ComponentInstance instance;
        private boolean destroyed;

        public ComponentManagedReference(final ComponentInstance component) {
            instance = component;
        }

        @Override
        public synchronized void release() {
            if (!destroyed) {
                instance.destroy();
                destroyed = true;
            }
        }

        @Override
        public Object getInstance() {
            return instance.getInstance();
        }
    }
}
