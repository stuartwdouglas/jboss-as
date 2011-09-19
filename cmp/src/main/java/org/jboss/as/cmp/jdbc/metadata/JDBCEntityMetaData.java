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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.cmp.jdbc.metadata.parser.JDBCMetaDataParser;
import org.jboss.metadata.ejb.spec.CMPFieldMetaData;
import org.jboss.metadata.ejb.spec.EntityBeanMetaData;
import org.jboss.metadata.ejb.spec.QueryMetaData;

/**
 * This immutable class contains information about an entity
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="mailto:dirk@jboss.de">Dirk Zimmermann</a>
 * @author <a href="mailto:loubyansky@hotmail.com">Alex Loubyansky</a>
 * @author <a href="mailto:heiko.rupp@cellent.de">Heiko W. Rupp</a>
 * @version $Revision: 81030 $
 */
public final class JDBCEntityMetaData {
    /**
     * application metadata in which this entity is defined
     */
    private JDBCApplicationMetaData jdbcApplication;

    /**
     * data source name in jndi
     */
    private String dataSourceName;

    /**
     * type mapping used for this entity
     */
    private String dataSourceMappingName;

    /**
     * the name of this entity
     */
    private String entityName;

    /**
     * the abstract schema name of this entity
     */
    private String abstractSchemaName;

    /**
     * the implementation class of this entity
     */
    private Class<?> entityClass;

    /**
     * the home class of this entity
     */
    private Class<?> homeClass;

    /**
     * the remote class of this entity
     */
    private Class<?> remoteClass;

    /**
     * the local home class of this entity
     */
    private Class<?> localHomeClass;

    /**
     * the local class of this entity
     */
    private Class<?> localClass;

    /**
     * Does this entity use cmp 1.x?
     */
    private Boolean isCMP1x;

    /**
     * the name of the table to which this entity is persisted
     */
    private String tableName;

    /**
     * Should we try and create the table when deployed?
     */
    private Boolean createTable;

    /**
     * Should we drop the table when undeployed?
     */
    private Boolean removeTable;

    /**
     * Should we alter the table when deployed?
     */
    private Boolean alterTable;

    /**
     * What command should be issued directly after creation
     * of a table?
     */
    private List<String> tablePostCreateCmd;

    /**
     * Should we use 'SELECT ... FOR UPDATE' syntax when loading?
     */
    private Boolean rowLocking;

    /**
     * Is this entity read-only?
     */
    private Boolean readOnly;

    /**
     * how long is a read valid
     */
    private Integer readTimeOut;

    /**
     * Should the table have a primary key constraint?
     */
    private Boolean primaryKeyConstraint;

    /**
     * the java class of the primary key
     */
    private Class<?> primaryKeyClass;

    /**
     * the name of the primary key field or null if the primary key field
     * is multivalued
     */
    private String primaryKeyFieldName;

    /**
     * Map of the cmp fields of this entity by field name.
     */
    private Map<String, JDBCCMPFieldMetaData> cmpFieldsByName = new HashMap<String, JDBCCMPFieldMetaData>();
    private List<JDBCCMPFieldMetaData> cmpFields = new ArrayList<JDBCCMPFieldMetaData>();

    /**
     * A map of all the load groups by name.
     */
    private Map<String, List<String>> loadGroups = new HashMap<String, List<String>>();

    /**
     * The fields which should always be loaded when an entity of this type
     * is loaded.
     */
    private String eagerLoadGroup;

    /**
     * A list of groups (also lists) of the fields that should be lazy
     * loaded together.
     */
    private List<String> lazyLoadGroups = new ArrayList<String>();

    /**
     * Map of the queries on this entity by the Method that invokes the query.
     */
    private Map<Method, JDBCQueryMetaData> queries = new HashMap<Method, JDBCQueryMetaData>();

    /**
     * The factory used to used to create query meta data
     */
    private JDBCQueryMetaDataFactory queryFactory;

    /**
     * The read ahead meta data
     */
    private JDBCReadAheadMetaData readAhead;

    /**
     * clean-read-ahead-on-load
     * Since 3.2.5RC1, previously read ahead cache was cleaned after loading.
     */
    private Boolean cleanReadAheadOnLoad;

    /**
     * The maximum number of read ahead lists that can be tracked for this
     * entity.
     */
    private Integer listCacheMax;

    /**
     * The number of entities to read in one round-trip to the
     * underlying data store.
     */
    private Integer fetchSize;

    /**
     * entity command meta data
     */
    private JDBCEntityCommandMetaData entityCommand;


    /**
     * optimistic locking metadata
     */
    private JDBCOptimisticLockingMetaData optimisticLocking;


    /**
     * audit metadata
     */
    private JDBCAuditMetaData audit;

    private Class<?> qlCompiler;

    /**
     * throw runtime exception metadata
     */
    private Boolean throwRuntimeExceptions;

    private JDBCCMPFieldMetaData upkField;
    private List<JDBCMetaDataParser.TempQueryMetaData> tempQueries = new ArrayList<JDBCMetaDataParser.TempQueryMetaData>();

    public JDBCEntityMetaData(final JDBCApplicationMetaData jdbcApplication) {
        this.jdbcApplication = jdbcApplication;
    }


    /**
     * Constructs jdbc entity meta data defined in the jdbcApplication and
     * with the data from the entity meta data which is loaded from the
     * ejb-jar.xml file.
     *
     * @param jdbcApplication the application in which this entity is defined
     * @param entity          the entity meta data for this entity that is loaded
     *                        from the ejb-jar.xml file
     */
    public JDBCEntityMetaData(JDBCApplicationMetaData jdbcApplication, EntityBeanMetaData entity) {
        this.jdbcApplication = jdbcApplication;
        entityName = entity.getEjbName();

        final ClassLoader classLoader = jdbcApplication.getClassLoader();
        try {
            entityClass = classLoader.loadClass(entity.getEjbClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load entity class", e);
        }
        try {
            primaryKeyClass = classLoader.loadClass(entity.getPrimKeyClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load primary key class", e);
        }

        isCMP1x = entity.isCMP1x();
        if (isCMP1x) {
            abstractSchemaName = (entity.getAbstractSchemaName() == null ? entityName : entity.getAbstractSchemaName());
        } else {
            abstractSchemaName = entity.getAbstractSchemaName();
        }

        primaryKeyFieldName = entity.getPrimKeyField();

        String home = entity.getHome();
        if (home != null) {
            try {
                homeClass = classLoader.loadClass(home);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load home class", e);
            }
            try {
                remoteClass = classLoader.loadClass(entity.getRemote());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load remote class", e);
            }
        } else {
            homeClass = null;
            remoteClass = null;
        }

        String localHome = entity.getLocalHome();
        if (localHome != null) {
            try {
                localHomeClass = classLoader.loadClass(localHome);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load local home class", e);
            }
            try {
                localClass = classLoader.loadClass(entity.getLocal());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load local class", e);
            }
        } else {
            // we must have a home or local home
            if (home == null) {
                throw new RuntimeException("Entity must have atleast a home or local home: " + entityName);
            }

            localHomeClass = null;
            localClass = null;
        }

        // we replace the . by _ because some dbs die on it...
        // the table name may be overridden in importXml(jbosscmp-jdbc.xml)
        tableName = entityName.replace('.', '_');

        // Warn: readTimeOut should be setup before cmp fields are created
        // otherwise readTimeOut in cmp fields will be set to 0 by default

        // build the metadata for the cmp fields now in case there is
        // no jbosscmp-jdbc.xml

        for (CMPFieldMetaData cmpFieldMetaData : entity.getCmpFields()) {
            JDBCCMPFieldMetaData cmpField = new JDBCCMPFieldMetaData(this, cmpFieldMetaData.getFieldName());
            cmpFields.add(cmpField);
            cmpFieldsByName.put(cmpFieldMetaData.getFieldName(), cmpField);
        }

        // AL: add unknown primary key if primaryKeyClass is Object
        // AL: this is set up only in this constructor
        // AL: because, AFAIK, others are called with default value
        // AL: produced by this one
        if (primaryKeyClass == java.lang.Object.class) {
            JDBCCMPFieldMetaData upkField = new JDBCCMPFieldMetaData(this);
            cmpFields.add(upkField);
            cmpFieldsByName.put(upkField.getFieldName(), upkField);
        }

        queryFactory = new JDBCQueryMetaDataFactory(this);
        for (QueryMetaData queryData : entity.getQueries()) {
            queries.putAll(queryFactory.createJDBCQueryMetaData(queryData));
        }
    }

    public JDBCEntityMetaData(JDBCApplicationMetaData jdbcApplication, JDBCEntityMetaData defaultValues) {
        this.jdbcApplication = jdbcApplication;
        entityName = defaultValues.entityName;
        entityClass = defaultValues.entityClass;
        primaryKeyClass = defaultValues.primaryKeyClass;
        isCMP1x = defaultValues.isCMP1x;
        primaryKeyFieldName = defaultValues.primaryKeyFieldName;
        homeClass = defaultValues.homeClass;
        remoteClass = defaultValues.remoteClass;
        localHomeClass = defaultValues.localHomeClass;
        localClass = defaultValues.localClass;
        abstractSchemaName = defaultValues.abstractSchemaName;
        dataSourceName = defaultValues.dataSourceName;
        dataSourceMappingName = defaultValues.dataSourceMappingName;
        tableName = defaultValues.tableName;
        createTable = defaultValues.createTable;
        removeTable = defaultValues.removeTable;
        alterTable = defaultValues.alterTable;
        tablePostCreateCmd = defaultValues.tablePostCreateCmd;
        readOnly = defaultValues.readOnly;
        readTimeOut = defaultValues.readTimeOut;
        rowLocking = defaultValues.rowLocking;
        primaryKeyConstraint = defaultValues.primaryKeyConstraint;
        listCacheMax = defaultValues.listCacheMax;
        fetchSize = defaultValues.fetchSize;
        entityCommand = defaultValues.entityCommand;
        qlCompiler = defaultValues.qlCompiler;
        throwRuntimeExceptions = defaultValues.throwRuntimeExceptions;

        for (JDBCCMPFieldMetaData cmpField : defaultValues.cmpFields) {
            JDBCCMPFieldMetaData newCmpField = new JDBCCMPFieldMetaData(this, cmpField);
            cmpFields.add(cmpField);
            cmpFieldsByName.put(cmpField.getFieldName(), newCmpField);
        }

        loadGroups.putAll(defaultValues.loadGroups);
        eagerLoadGroup = defaultValues.eagerLoadGroup;
        readAhead = defaultValues.readAhead;
        cleanReadAheadOnLoad = defaultValues.cleanReadAheadOnLoad;
        optimisticLocking = defaultValues.optimisticLocking;
        audit = defaultValues.audit;


        queryFactory = new JDBCQueryMetaDataFactory(this);
        for (JDBCQueryMetaData query : defaultValues.queries.values()) {
            queries.put(query.getMethod(), queryFactory.createJDBCQueryMetaData(query, readAhead, qlCompiler));
        }
    }

    public JDBCEntityMetaData(final JDBCApplicationMetaData applicationMetaData, final JDBCEntityMetaData defaultValues, final JDBCEntityMetaData newValues) {
        this.jdbcApplication = applicationMetaData;
        entityName = newValues.entityName != null ? newValues.entityName : defaultValues.entityName;
        entityClass = newValues.entityClass != null ? newValues.entityClass : defaultValues.entityClass;
        primaryKeyClass = newValues.primaryKeyClass != null ? newValues.primaryKeyClass : defaultValues.primaryKeyClass;
        isCMP1x = newValues.isCMP1x != null ? newValues.isCMP1x : defaultValues.isCMP1x;
        primaryKeyFieldName = newValues.primaryKeyFieldName != null ? newValues.primaryKeyFieldName : defaultValues.primaryKeyFieldName;
        homeClass = newValues.homeClass != null ? newValues.homeClass : defaultValues.homeClass;
        remoteClass = newValues.remoteClass != null ? newValues.remoteClass : defaultValues.remoteClass;
        localHomeClass = newValues.localHomeClass != null ? newValues.localHomeClass : defaultValues.localHomeClass;
        localClass = newValues.localClass != null ? newValues.localClass : defaultValues.localClass;

        abstractSchemaName = newValues.abstractSchemaName != null ? newValues.abstractSchemaName : defaultValues.abstractSchemaName;

        dataSourceName = newValues.dataSourceName != null ? newValues.dataSourceName : defaultValues.dataSourceName;
        dataSourceMappingName = newValues.dataSourceMappingName != null ? newValues.dataSourceMappingName : defaultValues.dataSourceMappingName;
        tableName = newValues.tableName != null ? newValues.tableName : defaultValues.tableName;

        createTable = newValues.createTable != null ? newValues.createTable : defaultValues.createTable;
        removeTable = newValues.removeTable != null ? newValues.removeTable : defaultValues.removeTable;
        alterTable = newValues.alterTable != null ? newValues.alterTable : defaultValues.alterTable;

        tablePostCreateCmd = newValues.tablePostCreateCmd != null ? newValues.tablePostCreateCmd : defaultValues.tablePostCreateCmd;
        readOnly = newValues.readOnly != null ? newValues.readOnly : defaultValues.readOnly;
        readTimeOut = newValues.readTimeOut != null ? newValues.readTimeOut : defaultValues.readTimeOut;
        rowLocking = newValues.readTimeOut != null ? newValues.rowLocking : defaultValues.rowLocking;
        primaryKeyConstraint = newValues.primaryKeyConstraint != null ? newValues.primaryKeyConstraint : defaultValues.primaryKeyConstraint;
        listCacheMax = newValues.listCacheMax != null ? newValues.listCacheMax : defaultValues.listCacheMax;
        fetchSize = newValues.fetchSize != null ? newValues.fetchSize : defaultValues.fetchSize;
        entityCommand = newValues.entityCommand != null ? newValues.entityCommand : defaultValues.entityCommand;
        qlCompiler = newValues.qlCompiler != null ? newValues.qlCompiler : defaultValues.qlCompiler;
        throwRuntimeExceptions = newValues.throwRuntimeExceptions != null ? newValues.throwRuntimeExceptions : defaultValues.throwRuntimeExceptions;

        for (JDBCCMPFieldMetaData cmpField : defaultValues.cmpFields) {
            JDBCCMPFieldMetaData newCmpField = new JDBCCMPFieldMetaData(this, cmpField);
            cmpFields.add(cmpField);
            cmpFieldsByName.put(cmpField.getFieldName(), newCmpField);
        }

        for (JDBCCMPFieldMetaData cmpField : newValues.cmpFields) {
            JDBCCMPFieldMetaData newCmpField = new JDBCCMPFieldMetaData(this, cmpField);
            cmpFields.add(cmpField);
            cmpFieldsByName.put(cmpField.getFieldName(), newCmpField);
        }

        loadGroups.putAll(defaultValues.loadGroups);
        loadGroups.putAll(newValues.loadGroups);
        eagerLoadGroup = newValues.eagerLoadGroup != null ? newValues.eagerLoadGroup : defaultValues.eagerLoadGroup;
        readAhead = newValues.readAhead != null ? newValues.readAhead : defaultValues.readAhead;
        cleanReadAheadOnLoad = newValues.cleanReadAheadOnLoad != null ? newValues.cleanReadAheadOnLoad : defaultValues.cleanReadAheadOnLoad;
        optimisticLocking = newValues.optimisticLocking != null ? newValues.optimisticLocking : defaultValues.optimisticLocking;
        audit = newValues.audit != null ? newValues.audit : defaultValues.audit;
    }

    public static JDBCEntityMetaData merge(final JDBCApplicationMetaData applicationMetaData, final JDBCEntityMetaData defaultValues, final JDBCEntityMetaData newValues) {
        final JDBCEntityMetaData jdbcEntityMetaData = new JDBCEntityMetaData(applicationMetaData);

        jdbcEntityMetaData.entityName = newValues.entityName != null ? newValues.entityName : defaultValues.entityName;
        jdbcEntityMetaData.entityClass = newValues.entityClass != null ? newValues.entityClass : defaultValues.entityClass;
        jdbcEntityMetaData.primaryKeyClass = newValues.primaryKeyClass != null ? newValues.primaryKeyClass : defaultValues.primaryKeyClass;
        jdbcEntityMetaData.isCMP1x = newValues.isCMP1x != null ? newValues.isCMP1x : defaultValues.isCMP1x;
        jdbcEntityMetaData.primaryKeyFieldName = newValues.primaryKeyFieldName != null ? newValues.primaryKeyFieldName : defaultValues.primaryKeyFieldName;
        jdbcEntityMetaData.homeClass = newValues.homeClass != null ? newValues.homeClass : defaultValues.homeClass;
        jdbcEntityMetaData.remoteClass = newValues.remoteClass != null ? newValues.remoteClass : defaultValues.remoteClass;
        jdbcEntityMetaData.localHomeClass = newValues.localHomeClass != null ? newValues.localHomeClass : defaultValues.localHomeClass;
        jdbcEntityMetaData.localClass = newValues.localClass != null ? newValues.localClass : defaultValues.localClass;

        jdbcEntityMetaData.abstractSchemaName = newValues.abstractSchemaName != null ? newValues.abstractSchemaName : defaultValues.abstractSchemaName;

        jdbcEntityMetaData.dataSourceName = newValues.dataSourceName != null ? newValues.dataSourceName : defaultValues.dataSourceName;
        jdbcEntityMetaData.dataSourceMappingName = newValues.dataSourceMappingName != null ? newValues.dataSourceMappingName : defaultValues.dataSourceMappingName;
        jdbcEntityMetaData.tableName = newValues.tableName != null ? newValues.tableName : defaultValues.tableName;

        jdbcEntityMetaData.createTable = mergeField(newValues.createTable, defaultValues.createTable, false);
        jdbcEntityMetaData.removeTable = mergeField(newValues.removeTable, defaultValues.removeTable, false);
        jdbcEntityMetaData.alterTable = mergeField(newValues.alterTable, defaultValues.alterTable, false);

        jdbcEntityMetaData.tablePostCreateCmd = newValues.tablePostCreateCmd != null ? newValues.tablePostCreateCmd : defaultValues.tablePostCreateCmd;
        jdbcEntityMetaData.readOnly = mergeField(newValues.readOnly, defaultValues.readOnly, false);
        jdbcEntityMetaData.readTimeOut = mergeField(newValues.readTimeOut, defaultValues.readTimeOut, -1);
        jdbcEntityMetaData.rowLocking = mergeField(newValues.rowLocking, defaultValues.rowLocking, false);
        jdbcEntityMetaData.primaryKeyConstraint = mergeField(newValues.primaryKeyConstraint != null, defaultValues.primaryKeyConstraint, false);
        jdbcEntityMetaData.listCacheMax = mergeField(newValues.listCacheMax, defaultValues.listCacheMax, 1000);
        jdbcEntityMetaData.fetchSize = mergeField(newValues.fetchSize, defaultValues.fetchSize, 0);
        jdbcEntityMetaData.entityCommand = newValues.entityCommand != null ? newValues.entityCommand : defaultValues.entityCommand;
        jdbcEntityMetaData.qlCompiler = newValues.qlCompiler != null ? newValues.qlCompiler : defaultValues.qlCompiler;
        jdbcEntityMetaData.throwRuntimeExceptions = mergeField(newValues.throwRuntimeExceptions, defaultValues.throwRuntimeExceptions, false);

        for (JDBCCMPFieldMetaData cmpField : defaultValues.cmpFields) {
            JDBCCMPFieldMetaData newCmpField = new JDBCCMPFieldMetaData(jdbcEntityMetaData, cmpField);
            jdbcEntityMetaData.cmpFields.add(cmpField);
            jdbcEntityMetaData.cmpFieldsByName.put(cmpField.getFieldName(), newCmpField);
        }

        for (JDBCCMPFieldMetaData cmpField : newValues.cmpFields) {
            JDBCCMPFieldMetaData newCmpField = new JDBCCMPFieldMetaData(jdbcEntityMetaData, cmpField);
            jdbcEntityMetaData.cmpFields.add(cmpField);
            jdbcEntityMetaData.cmpFieldsByName.put(cmpField.getFieldName(), newCmpField);
        }

        jdbcEntityMetaData.loadGroups.putAll(defaultValues.loadGroups);
        jdbcEntityMetaData.loadGroups.putAll(newValues.loadGroups);
        jdbcEntityMetaData.eagerLoadGroup = mergeField(newValues.eagerLoadGroup, defaultValues.eagerLoadGroup, "*");
        jdbcEntityMetaData.readAhead = mergeField(newValues.readAhead, defaultValues.readAhead, JDBCReadAheadMetaData.DEFAULT);
        jdbcEntityMetaData.cleanReadAheadOnLoad = mergeField(newValues.cleanReadAheadOnLoad, defaultValues.cleanReadAheadOnLoad, false);
        jdbcEntityMetaData.optimisticLocking = newValues.optimisticLocking != null ? newValues.optimisticLocking : defaultValues.optimisticLocking;
        jdbcEntityMetaData.audit = newValues.audit != null ? newValues.audit : defaultValues.audit;

        final JDBCQueryMetaDataFactory factory = new JDBCQueryMetaDataFactory(jdbcEntityMetaData);
        for (JDBCQueryMetaData queryMetaData : defaultValues.getQueries()) {
            JDBCQueryMetaData query = factory.createJDBCQueryMetaData(queryMetaData, jdbcEntityMetaData.readAhead, jdbcEntityMetaData.qlCompiler);
            jdbcEntityMetaData.queries.put(query.getMethod(), query);
        }
        for (JDBCQueryMetaData queryMetaData : newValues.getQueries()) {
            JDBCQueryMetaData query = factory.createJDBCQueryMetaData(queryMetaData, jdbcEntityMetaData.readAhead, jdbcEntityMetaData.qlCompiler);
            jdbcEntityMetaData.queries.put(query.getMethod(), query);
        }

        for (JDBCMetaDataParser.TempQueryMetaData tempQueryMetaData : defaultValues.tempQueries) {
            final List<JDBCQueryMetaData> builtQueries = factory.createJDBCQueryMetaData(tempQueryMetaData.getType(), tempQueryMetaData.getMethodName(), tempQueryMetaData.getMethodParams(), tempQueryMetaData.getQuery(),
                    tempQueryMetaData.getReadAheadMetaData(), tempQueryMetaData.getQlCompiler(), tempQueryMetaData.isLazyResultsetLoading(), tempQueryMetaData.getDeclaredParts());
            for (JDBCQueryMetaData queryMetaData : builtQueries) {
                jdbcEntityMetaData.addQuery(queryMetaData);
            }
        }
        for (JDBCMetaDataParser.TempQueryMetaData tempQueryMetaData : newValues.tempQueries) {
            final List<JDBCQueryMetaData> builtQueries = factory.createJDBCQueryMetaData(tempQueryMetaData.getType(), tempQueryMetaData.getMethodName(), tempQueryMetaData.getMethodParams(), tempQueryMetaData.getQuery(),
                    tempQueryMetaData.getReadAheadMetaData(), tempQueryMetaData.getQlCompiler(), tempQueryMetaData.isLazyResultsetLoading(), tempQueryMetaData.getDeclaredParts());
            for (JDBCQueryMetaData queryMetaData : builtQueries) {
                jdbcEntityMetaData.addQuery(queryMetaData);
            }
        }
        return jdbcEntityMetaData;
    }

    private static <T> T mergeField(T newValue, T oldValue, T defaultValue) {
        return newValue != null ? newValue : oldValue;  // Ignore default value for now..
    }

    /**
     * Gets the meta data for the application of which this entity is a member.
     *
     * @return the meta data for the application that this entity is a memeber
     */
    public JDBCApplicationMetaData getJDBCApplication() {
        return jdbcApplication;
    }

    /**
     * Gets the name of the datasource in jndi for this entity
     *
     * @return the name of datasource in jndi
     */
    public String getDataSourceName() {
        return dataSourceName;
    }

    /**
     * Gets the jdbc type mapping for this entity
     *
     * @return the jdbc type mapping for this entity
     */
    public JDBCTypeMappingMetaData getTypeMapping() {
        final JDBCTypeMappingMetaData typeMapping = jdbcApplication.getTypeMappingByName(dataSourceMappingName);
        if (typeMapping == null) {
            throw new RuntimeException("type-mapping is not initialized: " + dataSourceName
                    + " was not deployed or type-mapping was not configured.");
        }

        return typeMapping;
    }

    /**
     * Gets the name of this entity. The name come from the ejb-jar.xml file.
     *
     * @return the name of this entity
     */
    public String getName() {
        return entityName;
    }

    /**
     * Gets the abstract shcema name of this entity. The name come from
     * the ejb-jar.xml file.
     *
     * @return the abstract schema name of this entity
     */
    public String getAbstractSchemaName() {
        return abstractSchemaName;
    }

    /**
     * Gets the implementation class of this entity
     *
     * @return the implementation class of this entity
     */
    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Gets the home class of this entity
     *
     * @return the home class of this entity
     */
    public Class<?> getHomeClass() {
        return homeClass;
    }

    /**
     * Gets the remote class of this entity
     *
     * @return the remote class of this entity
     */
    public Class<?> getRemoteClass() {
        return remoteClass;
    }

    /**
     * Gets the local home class of this entity
     *
     * @return the local home class of this entity
     */
    public Class<?> getLocalHomeClass() {
        return localHomeClass;
    }

    /**
     * Gets the local class of this entity
     *
     * @return the local class of this entity
     */
    public Class<?> getLocalClass() {
        return localClass;
    }

    /**
     * Does this entity use CMP version 1.x
     *
     * @return true if this entity used CMP version 1.x; otherwise false
     */
    public boolean isCMP1x() {
        return isCMP1x;
    }

    /**
     * Gets the cmp fields of this entity
     *
     * @return an unmodifiable collection of JDBCCMPFieldMetaData objects
     */
    public List<JDBCCMPFieldMetaData> getCMPFields() {
        return Collections.unmodifiableList(cmpFields);
    }

    /**
     * Gets the name of the eager load group. This name can be used to
     * look up the load group.
     *
     * @return the name of the eager load group
     */
    public String getEagerLoadGroup() {
        return eagerLoadGroup;
    }

    /**
     * Gets the collection of lazy load group names.
     *
     * @return an unmodifiable collection of load group names
     */
    public List<String> getLazyLoadGroups() {
        return Collections.unmodifiableList(lazyLoadGroups);
    }

    /**
     * Gets the map from load grou name to a List of field names, which
     * forms a logical load group.
     *
     * @return an unmodifiable map of load groups (Lists) by group name.
     */
    public Map<String, List<String>> getLoadGroups() {
        return Collections.unmodifiableMap(loadGroups);
    }

    /**
     * Gets the load group with the specified name.
     *
     * @return the load group with the specified name
     */
    public List<String> getLoadGroup(String name) {
        List<String> group = loadGroups.get(name);
        if (group == null) {
            throw new RuntimeException("Unknown load group: name=" + name);
        }
        return group;
    }

    /**
     * Returns optimistic locking metadata
     */
    public JDBCOptimisticLockingMetaData getOptimisticLocking() {
        return optimisticLocking;
    }

    /**
     * Returns audit metadata
     */
    public JDBCAuditMetaData getAudit() {
        return audit;
    }

    /**
     * Gets the cmp field with the specified name
     *
     * @param name the name of the desired field
     * @return the cmp field with the specified name or null if not found
     */
    public JDBCCMPFieldMetaData getCMPFieldByName(String name) {
        return cmpFieldsByName.get(name);
    }

    /**
     * Gets the name of the table to which this entity is persisted
     *
     * @return the name of the table to which this entity is persisted
     */
    public String getDefaultTableName() {
        return tableName;
    }

    /**
     * Gets the flag used to determine if the store manager should attempt to
     * create database table when the entity is deployed.
     *
     * @return true if the store manager should attempt to create the table
     */
    public boolean getCreateTable() {
        return createTable;
    }

    /**
     * Gets the flag used to determine if the store manager should attempt to
     * remove database table when the entity is undeployed.
     *
     * @return true if the store manager should attempt to remove the table
     */
    public boolean getRemoveTable() {
        return removeTable;
    }

    /**
     * Gets the flag used to determine if the store manager should attempt to
     * alter table when the entity is deployed.
     */
    public boolean getAlterTable() {
        return alterTable != null && alterTable;
    }

    /**
     * Get the (user-defined) SQL commands that sould be issued after table
     * creation
     *
     * @return the SQL command to issue to the DB-server
     */
    public List<String> getDefaultTablePostCreateCmd() {
        return tablePostCreateCmd;
    }

    /**
     * Gets the flag used to determine if the store manager should add a
     * priary key constraint when creating the table
     *
     * @return true if the store manager should add a primary key constraint to
     *         the create table sql statement
     */
    public boolean hasPrimaryKeyConstraint() {
        return primaryKeyConstraint != null && primaryKeyConstraint;
    }

    /**
     * Gets the flag used to determine if the store manager should do row locking
     * when loading entity beans
     *
     * @return true if the store manager should add a row locking
     *         clause when selecting data from the table
     */
    public boolean hasRowLocking() {
        return rowLocking != null && rowLocking;
    }

    /**
     * The maximum number of qurey result lists that will be tracked.
     */
    public int getListCacheMax() {
        return listCacheMax != null ? listCacheMax : 1000;
    }

    /**
     * The number of rows that the database driver should get in a single
     * trip to the database.
     */
    public int getFetchSize() {
        return fetchSize != null ? fetchSize : 0;
    }


    /**
     * Gets the queries defined on this entity
     *
     * @return an unmodifiable collection of JDBCQueryMetaData objects
     */
    public Collection<JDBCQueryMetaData> getQueries() {
        return Collections.unmodifiableCollection(queries.values());
    }

    /**
     * @param method finder method name.
     * @return corresponding query metadata or null.
     */
    public JDBCQueryMetaData getQueryMetaDataForMethod(Method method) {
        return (JDBCQueryMetaData) queries.get(method);
    }

    /**
     * Get the relationsip roles of this entity.
     * Items are instance of JDBCRelationshipRoleMetaData.
     *
     * @return an unmodifiable collection of the relationship roles defined
     *         for this entity
     */
    public Collection<JDBCRelationshipRoleMetaData> getRelationshipRoles() {
        return jdbcApplication.getRolesForEntity(entityName);
    }

    /**
     * Gets the primary key class for this entity
     *
     * @return the primary key class for this entity
     */
    public Class<?> getPrimaryKeyClass() {
        return primaryKeyClass;
    }

    /**
     * Gets the entity command metadata
     *
     * @return the entity command metadata
     */
    public JDBCEntityCommandMetaData getEntityCommand() {
        return entityCommand;
    }

    /**
     * Is this entity read only? A readonly entity will never be stored into
     * the database.
     *
     * @return true if this entity is read only
     */
    public boolean isReadOnly() {
        return readOnly != null && readOnly;
    }

    /**
     * How long is a read of this entity valid. This property should only be
     * used on read only entities, and determines how long the data read from
     * the database is valid. When the read times out it should be reread from
     * the database. If the value is -1 and the entity is not using commit
     * option a, the read is only valid for the length of the transaction in
     * which it was loaded.
     *
     * @return the length of time that a read is valid or -1 if the read is only
     *         valid for the length of the transaction
     */
    public int getReadTimeOut() {
        return readTimeOut != null ? readTimeOut : -1;
    }

    /**
     * Gets the name of the primary key field of this entity or null if
     * the primary key is multivalued
     *
     * @return the name of the primary key field of this entity or null
     *         if the primary key is multivalued
     */
    public String getPrimaryKeyFieldName() {
        return primaryKeyFieldName;
    }


    /**
     * Gets the read ahead meta data for this entity.
     *
     * @return the read ahead meta data for this entity.
     */
    public JDBCReadAheadMetaData getReadAhead() {
        return readAhead;
    }

    public Class<?> getQlCompiler() {
        return qlCompiler;
    }

    /**
     * Is the throw-runtime-exceptions meta data for this entity is true.
     *
     * @return the throw-runtime-exceptions meta data for this entity.
     */
    public boolean isThrowRuntimeExceptions() {
        return throwRuntimeExceptions != null && throwRuntimeExceptions;
    }

    /**
     * Gets the throw-runtime-exceptions meta data for this entity.
     *
     * @return the throw-runtime-exceptions meta data for this entity.
     */
    public boolean getThrowRuntimeExceptions() {
        return throwRuntimeExceptions;
    }


    public boolean isCleanReadAheadOnLoad() {
        return cleanReadAheadOnLoad != null && cleanReadAheadOnLoad;
    }

    /**
     * Compares this JDBCEntityMetaData against the specified object. Returns
     * true if the objects are the same. Two JDBCEntityMetaData are the same
     * if they both have the same name and are defined in the same application.
     *
     * @param o the reference object with which to compare
     * @return true if this object is the same as the object argument;
     *         false otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof JDBCEntityMetaData) {
            JDBCEntityMetaData entity = (JDBCEntityMetaData) o;
            return entityName.equals(entity.entityName) &&
                    jdbcApplication.equals(entity.jdbcApplication);
        }
        return false;
    }

    /**
     * Returns a hashcode for this JDBCEntityMetaData. The hashcode is computed
     * based on the hashCode of the declaring application and the hashCode of
     * the entityName
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        int result = 17;
        result = 37 * result + jdbcApplication.hashCode();
        result = 37 * result + entityName.hashCode();
        return result;
    }

    /**
     * Returns a string describing this JDBCEntityMetaData. The exact details
     * of the representation are unspecified and subject to change, but the
     * following may be regarded as typical:
     * <p/>
     * "[JDBCEntityMetaData: entityName=UserEJB]"
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "[JDBCEntityMetaData : entityName=" + entityName + "]";
    }

    public void setDataSource(final String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public void setDataSourceMapping(final String datasourceMapping) {
        this.dataSourceMappingName = datasourceMapping;
    }

    public void setCreateTable(final boolean createTable) {
        this.createTable = createTable;
    }

    public void setAlterTable(final boolean alterTable) {
        this.alterTable = alterTable;
    }

    public void setThowRuntimeExceptions(final boolean throwRuntimeExceptions) {
        this.throwRuntimeExceptions = throwRuntimeExceptions;
    }

    public void setRemoveTable(final boolean removeTable) {
        this.removeTable = removeTable;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setReadTimeout(final int readTimeOut) {
        this.readTimeOut = readTimeOut;
    }

    public void setRowLocking(final boolean rowLocking) {
        this.rowLocking = rowLocking;
    }

    public void setPkConstraint(final boolean primaryKeyConstraint) {
        this.primaryKeyConstraint = primaryKeyConstraint;
    }

    public void setListCacheMax(final int listCacheMax) {
        this.listCacheMax = listCacheMax;
    }

    public void setCleanReadAheadOnLoad(final boolean cleanReadAheadOnLoad) {
        this.cleanReadAheadOnLoad = cleanReadAheadOnLoad;
    }

    public void setFetchSize(final int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public void setQlCompiler(final Class<?> qlCompiler) {
        this.qlCompiler = qlCompiler;
    }

    public void setReadAhead(final JDBCReadAheadMetaData readAhead) {
        this.readAhead = readAhead;
    }

    public void setUnknownPk(JDBCCMPFieldMetaData upkField) {
        this.upkField = upkField;
    }

    public void setEntityCommand(final JDBCEntityCommandMetaData entityCommand) {
        this.entityCommand = entityCommand;
    }

    public void addPostTableCreate(final String command) {
        this.tablePostCreateCmd.add(command);
    }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    public void setEagerLoadGroup(final String eagerLoadGroup) {
        this.eagerLoadGroup = eagerLoadGroup;
    }

    public void addLazyLoadGroup(final String group) {
        this.lazyLoadGroups.add(group);
    }

    public void setOptimisticLocking(final JDBCOptimisticLockingMetaData optimisticLocking) {
        this.optimisticLocking = optimisticLocking;
    }

    public void setAudit(final JDBCAuditMetaData audit) {
        this.audit = audit;
    }

    public void setEntityName(final String entityName) {
        this.entityName = entityName;
    }

    public void addCmpField(final JDBCCMPFieldMetaData cmpField) {
        this.cmpFields.add(cmpField);
        this.cmpFieldsByName.put(cmpField.getFieldName(), cmpField);
    }

    public void addLoadGroup(final String groupName, final List<String> fields) {
        if (!loadGroups.containsKey(groupName)) {
            loadGroups.put(groupName, new ArrayList<String>());
        }
        loadGroups.get(groupName).addAll(fields);
    }

    public void setFkConstraint(boolean b) {
    }

    public void addQuery(JDBCQueryMetaData queryMetaData) {
        queries.put(queryMetaData.getMethod(), queryMetaData);
    }

    public void addTempQueryMetaData(JDBCMetaDataParser.TempQueryMetaData tempQueryMetaData) {
        this.tempQueries.add(tempQueryMetaData);
    }
}
