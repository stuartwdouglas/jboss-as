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
package org.jboss.as.ejb3.entitycache;

import javax.ejb.NoSuchEntityException;

/**
 * Creates and destroys entities that maintain their own external identity.
 *
 * @author Stuart Douglas
 */
public interface EntityObjectFactory<T> {

    /**
     * Create a new instance of this component, an initalize it with the given identity.
     *
     * Note that this may retrieve an object from a pool and initalize it, rather than creating a new instance.
     *
     * @return the component instance
     */
    T createInstance(Object identity) throws NoSuchEntityException;

    /**
     * Release an instance of the component. The component will be disassociated with the given identity, and may be returned
     * to the pool.
     *
     * @param instance the instance to destroy
     */
    void destroyInstance(T instance);

}
