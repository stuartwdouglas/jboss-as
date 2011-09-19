/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.metadata;

/**
 * Optimistick locking metadata
 *
 * @author <a href="mailto:aloubyansky@hotmail.com">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCOptimisticLockingMetaData {

    // Constants ---------------------------------------
    public enum LockingStrategy {
        FIELD_GROUP_STRATEGY, MODIFIED_STRATEGY, READ_STRATEGY, VERSION_COLUMN_STRATEGY, TIMESTAMP_COLUMN_STRATEGY, KEYGENERATOR_COLUMN_STRATEGY;
    }

    // Attributes --------------------------------------
    /**
     * locking strategy
     */
    private LockingStrategy lockingStrategy;

    /**
     * group name for field group strategy
     */
    private String groupName;

    /**
     * locking field for verion- or timestamp-column strategy
     */
    private JDBCCMPFieldMetaData lockingField;

    /**
     * key generator factory
     */
    private String keyGeneratorFactory;

    // Public ------------------------------------------
    public LockingStrategy getLockingStrategy() {
        return lockingStrategy;
    }

    public String getGroupName() {
        return groupName;
    }

    public JDBCCMPFieldMetaData getLockingField() {
        return lockingField;
    }

    public String getKeyGeneratorFactory() {
        return keyGeneratorFactory;
    }

    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    public void setLockingStrategy(final LockingStrategy lockingStrategy) {
        this.lockingStrategy = lockingStrategy;
    }

    public void setLockingField(JDBCCMPFieldMetaData lockingField) {
        this.lockingField = lockingField;
    }

    public void setKeyGeneratorFactory(final String keyGeneratorFactory) {
        this.keyGeneratorFactory = keyGeneratorFactory;
    }
}
