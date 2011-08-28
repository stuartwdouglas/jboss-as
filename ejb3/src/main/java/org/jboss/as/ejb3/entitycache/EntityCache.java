/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.entitycache;

import javax.ejb.NoSuchEntityException;

/**
 * A cache for stateful objects who's state is maintained externally, namely entity beans.
 *
 * @author Stuart Douglas
 */
public interface EntityCache<T> {


    /**
     * Called after an entity bean has been created and associated with a new identity.
     *
     * This corresponds to an ejbCreate call on the entity bean.
     *
     * The newly created object will not be marked as in use. To invoke business methods
     * on it it should still be acquired using get()
     *
     * @param instance The new instance
     */
    void create(Object key, T instance);

    /**
     * Get the specified object from cache. This will mark
     * the object as being in use. If the object is not found one will be
     * created and initialized with the identity
     *
     * @param key the identifier of the object
     * @return the object
     * @throws javax.ejb.NoSuchEntityException if the object identity association failed
     */
    T get(Object key) throws NoSuchEntityException;

    /**
     * Release the object from use.
     *
     * @param obj the object
     */
    void release(Object primaryKey);

    /**
     * Discard the object, called when an exception occurs
     * @param primaryKey
     */
    void discard(Object primaryKey);

    /**
     * Remove the specified object. This corresponds to an entity being
     * deleted from the database, and as such the object should be releaed
     * back into the pool immediately.
     *
     * @param key the identifier of the object
     */
    void remove(Object key);

    /**
     * Start the cache.
     */
    void start();

    /**
     * Stop the cache.
     */
    void stop();
}
