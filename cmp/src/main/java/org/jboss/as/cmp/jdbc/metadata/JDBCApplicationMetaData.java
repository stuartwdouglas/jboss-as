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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EntityBeanMetaData;
import org.jboss.metadata.ejb.spec.RelationMetaData;

/**
 * This class contains information about the application
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCApplicationMetaData {
    /**
     * The class loader for this application.  The class loader is used to
     * load all classes used by this application.
     */
    private final ClassLoader classLoader;

    /**
     * Map with user defined type mapping, e.g. enum mappings
     */
    private final Map<String, JDBCUserTypeMappingMetaData> userTypeMappings = new HashMap<String, JDBCUserTypeMappingMetaData>();

    /**
     * Map of the type mappings by name.
     */
    private final Map<String, JDBCTypeMappingMetaData> typeMappings = new HashMap<String, JDBCTypeMappingMetaData>();

    /**
     * Map of the entities managed by jbosscmp-jdbc by bean name.
     */
    private final Map<String, JDBCEntityMetaData> entities = new HashMap<String, JDBCEntityMetaData>();

    /**
     * Collection of relations in this application.
     */
    private final Map<String, JDBCRelationMetaData> relationships = new HashMap<String, JDBCRelationMetaData>();

    /**
     * Map of the collection relationship roles for each entity by entity object.
     */
    private final Map<String, Set<JDBCRelationshipRoleMetaData>> entityRoles = new HashMap<String, Set<JDBCRelationshipRoleMetaData>>();

    /**
     * Map of the dependent value classes by java class type.
     */
    private final Map<Class<?>, JDBCValueClassMetaData> valueClasses = new HashMap<Class<?>, JDBCValueClassMetaData>();

    /**
     * Map from abstract schema name to entity name
     */
    private final Map<String, JDBCEntityMetaData> entitiesByAbstractSchemaName = new HashMap<String, JDBCEntityMetaData>();

    /**
     * Map from entity interface(s) java type to entity name
     */
    private final Map<Class<?>, JDBCEntityMetaData> entitiesByInterface = new HashMap<Class<?>, JDBCEntityMetaData>();

    /**
     * Map of the entity commands by name.
     */
    private final Map<String, JDBCEntityCommandMetaData> entityCommands = new HashMap<String, JDBCEntityCommandMetaData>();

    private JDBCEntityMetaData defaultEntity;

    public JDBCApplicationMetaData(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public JDBCApplicationMetaData(final EjbJarMetaData ejbJarMetaData, final ClassLoader classLoader) {
        this.classLoader = classLoader;

        for (EnterpriseBeanMetaData bean : ejbJarMetaData.getEnterpriseBeans()) {
            // only take entities
            if (bean.isEntity()) {
                final EntityBeanMetaData entity = EntityBeanMetaData.class.cast(bean);
                if (entity.isCMP()) {
                    JDBCEntityMetaData jdbcEntity = new JDBCEntityMetaData(this, entity);

                    entities.put(entity.getEjbName(), jdbcEntity);

                    String schemaName = jdbcEntity.getAbstractSchemaName();
                    if (schemaName != null) {
                        entitiesByAbstractSchemaName.put(schemaName, jdbcEntity);
                    }

                    final Class<?> remote = jdbcEntity.getRemoteClass();
                    if (remote != null) {
                        entitiesByInterface.put(remote, jdbcEntity);
                    }

                    final Class<?> local = jdbcEntity.getLocalClass();
                    if (local != null) {
                        entitiesByInterface.put(local, jdbcEntity);
                    }

                    // initialized the entity roles collection
                    entityRoles.put(entity.getEjbName(), new HashSet<JDBCRelationshipRoleMetaData>());
                }
            }
        }

        // relationships
        if (ejbJarMetaData.getRelationships() != null)
            for (RelationMetaData relationMetaData : ejbJarMetaData.getRelationships()) {
                // Relationship metadata
                JDBCRelationMetaData jdbcRelation = new JDBCRelationMetaData(this, relationMetaData);
                relationships.put(jdbcRelation.getRelationName(), jdbcRelation);

                // Left relationship-role metadata
                JDBCRelationshipRoleMetaData left = jdbcRelation.getLeftRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> leftEntityRoles = entityRoles.get(left.getEntity().getName());
                leftEntityRoles.add(left);

                // Right relationship-role metadata
                JDBCRelationshipRoleMetaData right = jdbcRelation.getRightRelationshipRole();
                Set<JDBCRelationshipRoleMetaData> rightEntityRoles = entityRoles.get(right.getEntity().getName());
                rightEntityRoles.add(right);
            }
    }

    /**
     * Gets the type mapping with the specified name
     *
     * @param name the name for the type mapping
     * @return the matching type mapping or null if not found
     */
    public JDBCTypeMappingMetaData getTypeMappingByName(String name) {
        return typeMappings.get(name);
    }

    /**
     * Gets the relationship roles for the entity with the specified name.
     *
     * @param entityName the name of the entity whos roles are returned
     * @return an unmodifiable collection of JDBCRelationshipRoles
     *         of the specified entity
     */
    public Collection<JDBCRelationshipRoleMetaData> getRolesForEntity(String entityName) {
        Collection<JDBCRelationshipRoleMetaData> roles = entityRoles.get(entityName);
        return Collections.unmodifiableCollection(roles);
    }

    /**
     * Gets dependent value classes that are directly managed by the container.
     *
     * @returns an unmodifiable collection of JDBCValueClassMetaData
     */
    public Collection<JDBCValueClassMetaData> getValueClasses() {
        return Collections.unmodifiableCollection(valueClasses.values());
    }

    /**
     * Gets the metadata for an entity bean by name.
     *
     * @param name the name of the entity meta data to return
     * @return the entity meta data for the specified name
     */
    public JDBCEntityMetaData getBeanByEjbName(String name) {
        return entities.get(name);
    }

    /**
     * Gets the entity command with the specified name
     *
     * @param name the name for the entity-command
     * @return the matching entity command or null if not found
     */
    public JDBCEntityCommandMetaData getEntityCommandByName(final String name) {
        return entityCommands.get(name);
    }

    public Map<String, JDBCUserTypeMappingMetaData> getUserTypeMappings() {
        return Collections.unmodifiableMap(userTypeMappings);
    }

    public void addTypeMapping(final JDBCTypeMappingMetaData metaData) {
        typeMappings.put(metaData.getName(), metaData);
    }

    public void addEntityCommand(final JDBCEntityCommandMetaData entityCommand) {
        this.entityCommands.put(entityCommand.getCommandName(), entityCommand);
    }

    public void addRelationship(final JDBCRelationMetaData relationMetaData) {
        this.relationships.put(relationMetaData.getRelationName(), relationMetaData);
    }

    public void addEntity(final JDBCEntityMetaData entityMetaData) {
        this.entities.put(entityMetaData.getName(), entityMetaData);
        if (entityMetaData.getRemoteClass() != null) {
            this.entitiesByInterface.put(entityMetaData.getRemoteClass(), entityMetaData);
        }
        if (entityMetaData.getLocalClass() != null) {
            this.entitiesByInterface.put(entityMetaData.getLocalClass(), entityMetaData);
        }
    }

    public void addValueClass(final JDBCValueClassMetaData jdbcValueClassMetaData) {
        this.valueClasses.put(jdbcValueClassMetaData.getJavaType(), jdbcValueClassMetaData);
    }

    public void addUserTypeMapping(final JDBCUserTypeMappingMetaData jdbcUserTypeMappingMetaData) {
        this.userTypeMappings.put(jdbcUserTypeMappingMetaData.getJavaType(), jdbcUserTypeMappingMetaData);
    }

    public void addTypeMappings(final Collection<JDBCTypeMappingMetaData> jdbcTypeMappingMetaDatas) {
        for (JDBCTypeMappingMetaData typeMappingMetaData : jdbcTypeMappingMetaDatas) {
            addTypeMapping(typeMappingMetaData);
        }
    }

    public Collection<JDBCTypeMappingMetaData> getTypeMappings() {
        return typeMappings.values();
    }

    public void addUserTypeMappings(final List<JDBCUserTypeMappingMetaData> userTypeMappings) {
        for (JDBCUserTypeMappingMetaData userTypeMappingMetaData : userTypeMappings) {
            addUserTypeMapping(userTypeMappingMetaData);
        }
    }

    public void setUserTypeMappings(final Map<String, JDBCUserTypeMappingMetaData> userTypeMappings) {
        this.userTypeMappings.putAll(userTypeMappings);
    }

    public void addValueClasses(final Collection<JDBCValueClassMetaData> valueClasses) {
        for (JDBCValueClassMetaData valueClass : valueClasses) {
            addValueClass(valueClass);
        }
    }

    public Map<String, JDBCEntityCommandMetaData> getEntityCommands() {
        return Collections.unmodifiableMap(entityCommands);
    }

    public void addEntityCommands(final Map<String, JDBCEntityCommandMetaData> commands) {
        this.entityCommands.putAll(commands);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Collection<JDBCEntityMetaData> getBeans() {
        return entities.values();
    }

    public void getRelationship(String relationName) {
        this.relationships.get(relationName);
    }

    public JDBCEntityMetaData getDefaultEntity() {
        return defaultEntity;
    }

    public void setDefaultEntity(JDBCEntityMetaData defaultEntity) {
        this.defaultEntity = defaultEntity;
    }

    public static JDBCApplicationMetaData merge(final JDBCApplicationMetaData defaultValues, final JDBCApplicationMetaData newValues) {
        JDBCApplicationMetaData metaData = new JDBCApplicationMetaData(newValues.getClassLoader());

        metaData.userTypeMappings.putAll(defaultValues.getUserTypeMappings());
        metaData.userTypeMappings.putAll(newValues.getUserTypeMappings());

        metaData.typeMappings.putAll(defaultValues.typeMappings);
        metaData.typeMappings.putAll(newValues.typeMappings);

        metaData.valueClasses.putAll(defaultValues.valueClasses);
        metaData.valueClasses.putAll(newValues.valueClasses);

        metaData.entityCommands.putAll(defaultValues.entityCommands);
        metaData.entityCommands.putAll(newValues.entityCommands);

        if(newValues.getDefaultEntity() != null) {
            if(defaultValues.defaultEntity != null) {
                metaData.defaultEntity = JDBCEntityMetaData.merge(metaData, defaultValues.defaultEntity, newValues.defaultEntity);
            } else {
                metaData.defaultEntity = newValues.defaultEntity;
            }
        } else {
            metaData.defaultEntity = defaultValues.defaultEntity;
        }

        // First all the defaults
        for (JDBCEntityMetaData entity : defaultValues.getBeans()) {
            metaData.addEntity(new JDBCEntityMetaData(metaData, entity));
        }
        // Now the new ones
        for (JDBCEntityMetaData entity : newValues.entities.values()) {
            JDBCEntityMetaData existing = metaData.getBeanByEjbName(entity.getName());
            if (existing == null) {
                throw new RuntimeException("Configuration found in " +
                        "jbosscmp-jdbc.xml for entity " + entity.getName() + " but bean " +
                        "is not a jbosscmp-jdbc-managed cmp entity in " +
                        "ejb-jar.xml");
            }
            JDBCEntityMetaData newEntity = JDBCEntityMetaData.merge(metaData, existing, entity);
            newEntity = JDBCEntityMetaData.merge(metaData, metaData.defaultEntity, newEntity);

            metaData.entities.put(newEntity.getName(), newEntity);
            if (newEntity.getAbstractSchemaName() != null) {
                metaData.entitiesByAbstractSchemaName.put(newEntity.getAbstractSchemaName(), newEntity);
            }
            final Class<?> remote = newEntity.getRemoteClass();
            if (remote != null) {
                metaData.entitiesByInterface.put(remote, newEntity);
            }
            final Class<?> local = newEntity.getLocalClass();
            if (local != null) {
                metaData.entitiesByInterface.put(local, newEntity);
            }
        }

        for (JDBCEntityMetaData entity : metaData.entities.values()) {
            metaData.entityRoles.put(entity.getName(), new HashSet<JDBCRelationshipRoleMetaData>());
        }

        for (JDBCRelationMetaData relationMetaData : defaultValues.relationships.values()) {
            relationMetaData = new JDBCRelationMetaData(metaData, relationMetaData);
            metaData.relationships.put(relationMetaData.getRelationName(), relationMetaData);

            JDBCRelationshipRoleMetaData left = relationMetaData.getLeftRelationshipRole();
            Collection<JDBCRelationshipRoleMetaData> leftEntityRoles = metaData.entityRoles.get(left.getEntity().getName());
            leftEntityRoles.add(left);

            JDBCRelationshipRoleMetaData right = relationMetaData.getRightRelationshipRole();
            Collection<JDBCRelationshipRoleMetaData> rightEntityRoles = metaData.entityRoles.get(right.getEntity().getName());
            rightEntityRoles.add(right);
        }

        for (JDBCRelationMetaData relationMetaData : newValues.relationships.values()) {
            final JDBCRelationMetaData oldRelation = metaData.relationships.get(relationMetaData.getRelationName());
            if (oldRelation == null) {
                throw new RuntimeException("Configuration found in " +
                        "jbosscmp-jdbc.xml for relation " + relationMetaData.getRelationName() +
                        " but relation is not a jbosscmp-jdbc-managed relation " +
                        "in ejb-jar.xml");
            }

            relationMetaData = JDBCRelationMetaData.merge(metaData, oldRelation, relationMetaData);

            metaData.relationships.put(relationMetaData.getRelationName(), relationMetaData);

            JDBCRelationshipRoleMetaData left = relationMetaData.getLeftRelationshipRole();
            Collection<JDBCRelationshipRoleMetaData> leftEntityRoles = metaData.entityRoles.get(left.getEntity().getName());
            leftEntityRoles.add(left);

            JDBCRelationshipRoleMetaData right = relationMetaData.getRightRelationshipRole();
            Collection<JDBCRelationshipRoleMetaData> rightEntityRoles = metaData.entityRoles.get(right.getEntity().getName());
            rightEntityRoles.add(right);
        }
        return metaData;
    }
}
