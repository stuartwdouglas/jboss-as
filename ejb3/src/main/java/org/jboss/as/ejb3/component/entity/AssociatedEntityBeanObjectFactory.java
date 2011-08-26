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

import org.jboss.as.ejb3.entitycache.EntityObjectFactory;
import org.jboss.as.ejb3.pool.Pool;

/**
 * Associating object factory that pulls entity beans from a pool.
 *
 * When they are present in the cache the entity bean is in the ready state, and is associated with an
 * object identity. When it has been returned to the pool it is in the pooled state.
 *
 * @author Stuart Douglas
 */
public class AssociatedEntityBeanObjectFactory implements EntityObjectFactory<EntityBeanComponentInstance> {

    private final Pool<EntityBeanComponentInstance> pool;

    public AssociatedEntityBeanObjectFactory(final Pool<EntityBeanComponentInstance> pool) {
        this.pool = pool;
    }

    @Override
    public EntityBeanComponentInstance createInstance(final Object identity) {
        final EntityBeanComponentInstance instance = pool.get();
        instance.associate(identity);
        return instance;
    }

    @Override
    public void destroyInstance(final EntityBeanComponentInstance instance) {
        instance.deassociate();
        pool.release(instance);
    }
}
