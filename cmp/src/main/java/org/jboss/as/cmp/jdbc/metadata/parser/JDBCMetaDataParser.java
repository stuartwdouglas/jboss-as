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

package org.jboss.as.cmp.jdbc.metadata.parser;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.as.cmp.jdbc.SQLUtil;
import org.jboss.as.cmp.jdbc.metadata.JDBCApplicationMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCAuditMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCCMPFieldPropertyMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCFunctionMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCLeftJoinMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCOptimisticLockingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaDataFactory;
import org.jboss.as.cmp.jdbc.metadata.JDBCReadAheadMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCUserTypeMappingMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCValueClassMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCValuePropertyMetaData;
import org.jboss.metadata.parser.util.MetaDataElementParser;

/**
 * @author John Bailey
 */
public class JDBCMetaDataParser extends MetaDataElementParser {

    public static JDBCApplicationMetaData parse(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        moveToStart(reader);
        final JDBCApplicationMetaData applicationMetaData = new JDBCApplicationMetaData(classLoader);

        List<JDBCUserTypeMappingMetaData> userTypeMappings = null;
        for (Element element : children(reader)) {
            switch (element) {
                case DEFAULTS: {
                    applicationMetaData.setDefaultEntity(parseDefaults(reader, applicationMetaData));
                    break;
                }
                case RELATIONSHIPS: {
                    parseRelationships(reader, applicationMetaData);
                    break;
                }
                case ENTERPRISE_BEANS: {
                    parseEnterpriseBeans(reader, applicationMetaData);
                    break;
                }
                case TYPE_MAPPINGS: {
                    applicationMetaData.addTypeMappings(parseTypeMappings(reader));
                    break;
                }
                case ENTITY_COMMANDS: {
                    parseEntityCommands(reader, applicationMetaData);
                    break;
                }
                case DEPENDENT_VALUE_CLASSES: {
                    parseDependentValueClasses(reader, applicationMetaData);
                    break;
                }
                case USER_TYPE_MAPPINGS: {
                    userTypeMappings = parseUserTypeMappings(reader, applicationMetaData);
                    break;
                }
                case RESERVED_WORDS: {
                    parseReservedWords(reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (userTypeMappings != null) {
            applicationMetaData.addUserTypeMappings(userTypeMappings);
        }
        return applicationMetaData;
    }

    private static List<JDBCTypeMappingMetaData> parseTypeMappings(final XMLStreamReader reader) throws XMLStreamException {
        final List<JDBCTypeMappingMetaData> typeMappings = new ArrayList<JDBCTypeMappingMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case TYPE_MAPPING: {
                    typeMappings.add(parseTypeMapping(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return typeMappings;
    }

    private static JDBCTypeMappingMetaData parseTypeMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCTypeMappingMetaData metaData = new JDBCTypeMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case NAME: {
                    metaData.setName(getElementText(reader));
                    break;
                }
                case ADD_COLUMN_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAddColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", value));
                    } else {
                        metaData.setAddColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", "ALTER TABLE ?1 ADD ?2 ?3"));
                    }
                    break;
                }
                case ALIAS_HEADER_PREFIX: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAliasHeaderPrefix(value);
                    }
                    break;
                }
                case ALIAS_HEADER_SUFFIX: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAliasHeaderSuffix(value);
                    }
                    break;
                }
                case ALIAS_MAX_LENGHT: {
                    final String value = getElementText(reader);
                    try {
                        final int aliasMaxLength = Integer.parseInt(value);
                        metaData.setAliasMaxLength(aliasMaxLength);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number format in alias-max-length " + value + "': " + e);
                    }
                    break;
                }
                case ALTER_COLUMN_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAlterColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", value));
                    } else {
                        metaData.setAlterColomnTemplate(new JDBCFunctionMappingMetaData("add-column-template", "ALTER TABLE ?1 ADD ?2 ?3"));
                    }
                    break;
                }
                case AUTO_INCREMENT_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setAutoIncrementTemplate(new JDBCFunctionMappingMetaData("auto-increment", value));
                    }
                    break;
                }
                case DROP_COLUMN_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setDropColomnTemplate(new JDBCFunctionMappingMetaData("drop-column-template", value));
                    } else {
                        metaData.setDropColomnTemplate(new JDBCFunctionMappingMetaData("drop-column-template", "ALTER TABLE ?1 DROP ?2"));
                    }
                    break;
                }
                case FALSE_MAPPING: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setFalseMapping(value);
                    }
                    break;
                }
                case FK_CONSTRAINT_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setFKConstraintTemplate(new JDBCFunctionMappingMetaData("fk-constraint", value));
                    }
                    break;
                }
                case FUNCTION_MAPPING: {
                    metaData.addFunctionMapping(parseFuctionMapping(reader));
                    break;
                }
                case MAPPING: {
                    metaData.addMapping(parseMapping(reader));
                    break;
                }
                case MAX_KEYS_IN_DELETE: {
                    final String value = getElementText(reader);
                    try {
                        final int maxKeys = Integer.parseInt(value);
                        metaData.setMaxKeysInDelete(maxKeys);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid number format in max-keys-in-delete " + value + "': " + e);
                    }
                    break;
                }
                case PK_CONSTRAINT_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setPKConstraintTemplate(new JDBCFunctionMappingMetaData("pk-constraint", value));
                    }
                    break;
                }
                case ROW_LOCKING_TEMPLATE: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setRowLockingTemplate(new JDBCFunctionMappingMetaData("row-locking", value));
                    }
                    break;
                }
                case SUBQUERY_SUPPORTED: {
                    metaData.setSubQuerySupported(Boolean.valueOf(getElementText(reader)));
                    break;
                }
                case TRUE_MAPPING: {
                    final String value = getElementText(reader);
                    if (!isEmpty(value)) {
                        metaData.setTrueMapping(value);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return metaData;
    }

    private static JDBCFunctionMappingMetaData parseFuctionMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCFunctionMappingMetaData metaData = new JDBCFunctionMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case FUNCTION_NAME: {
                    metaData.setFunctionName(getElementText(reader));
                    break;
                }
                case FUNCTION_SQL: {
                    metaData.setFunctionSql(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static JDBCMappingMetaData parseMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCMappingMetaData metaData = new JDBCMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case JAVA_TYPE: {
                    metaData.setJavaType(getElementText(reader));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                case PARAM_SETTER: {
                    metaData.setParamSetter(getElementText(reader));
                    break;
                }
                case RESULT_READER: {
                    metaData.setResultReader(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static JDBCEntityMetaData parseDefaults(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {
        final JDBCEntityMetaData defaultEntity = new JDBCEntityMetaData(applicationMetaData);

        for (Element element : children(reader)) {
            switch (element) {
                case DATASOURCE: {
                    defaultEntity.setDataSource(getElementText(reader));
                    break;
                }
                case DATASOURCE_MAPPING: {
                    defaultEntity.setDataSourceMapping(getElementText(reader));
                    break;
                }
                case CREATE_TABLE: {
                    defaultEntity.setCreateTable(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case ALTER_TABLE: {
                    defaultEntity.setAlterTable(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case REMOVE_TABLE: {
                    defaultEntity.setRemoveTable(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case POST_TABLE_CREATE: {
                    break;
                }
                case READ_ONLY: {
                    defaultEntity.setReadOnly(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case READ_TIMEOUT: {
                    defaultEntity.setReadTimeout(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case ROW_LOCKING: {
                    defaultEntity.setRowLocking(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case PK_CONSTRAINT: {
                    defaultEntity.setPkConstraint(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case FK_CONSTRAINT: {
                    getElementText(reader);
                    break;
                }
                case READ_AHEAD: {
                    defaultEntity.setReadAhead(parseReadAhead(reader));
                    break;
                }
                case LIST_CACHE_MAX: {
                    defaultEntity.setListCacheMax(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case CLEAN_READ_AHEAD: {
                    defaultEntity.setCleanReadAheadOnLoad(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case FETCH_SIZE: {
                    defaultEntity.setFetchSize(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case UNKNOWN_PK: {
                    defaultEntity.setUnknownPk(parseUnknownPk(reader, applicationMetaData.getClassLoader()));
                    break;
                }
                case ENTITY_COMMAND: {
                    defaultEntity.setEntityCommand(parseEntityCommand(reader, applicationMetaData.getClassLoader()));
                    break;
                }
                case QL_COMPILER: {
                    final String qlCompiler = getElementText(reader);
                    try {
                        defaultEntity.setQlCompiler(applicationMetaData.getClassLoader().loadClass(qlCompiler));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load compiler implementation: " + qlCompiler, e);
                    }

                    break;
                }
                case THROW_RUNTIME_EX: {
                    defaultEntity.setThowRuntimeExceptions(Boolean.valueOf(getElementText(reader)));
                    break;
                }
                case PREFERED_RELATION: {
                    getElementText(reader); // TODO: How to handle this
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return defaultEntity;
    }

    private static JDBCReadAheadMetaData parseReadAhead(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCReadAheadMetaData metaData = new JDBCReadAheadMetaData();
        final List<JDBCLeftJoinMetaData> leftJoins = new ArrayList<JDBCLeftJoinMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case STRATEGY: {
                    metaData.setStrategy(getElementText(reader));
                    break;
                }
                case PAGE_SIZE: {
                    metaData.setPageSize(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case EAGER_LOAD_GROUP: {
                    metaData.setEagerLoadGroup(getElementText(reader));
                    break;
                }
                case LEFT_JOIN: {
                    leftJoins.add(parseLeftJoin(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        metaData.setLeftJoins(leftJoins);
        return metaData;
    }

    private static JDBCLeftJoinMetaData parseLeftJoin(XMLStreamReader reader) throws XMLStreamException {
        final JDBCLeftJoinMetaData metaData = new JDBCLeftJoinMetaData();
        final List<JDBCLeftJoinMetaData> leftJoins = new ArrayList<JDBCLeftJoinMetaData>();

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CMR_FIELD: {
                    metaData.setCmrField(reader.getAttributeValue(i));
                    break;
                }
                case EAGER_LOAD_GROUP: {
                    metaData.setEagerLoadGroup(reader.getAttributeValue(i));
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        for (Element element : children(reader)) {
            switch (element) {
                case LEFT_JOIN: {
                    leftJoins.add(parseLeftJoin(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        metaData.setLeftJoins(leftJoins);
        return metaData;
    }

    private static JDBCCMPFieldMetaData parseUnknownPk(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {
        final JDBCCMPFieldMetaData metaData = new JDBCCMPFieldMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case UNKNOWN_KEY_CLASS: {
                    try {
                        metaData.setFieldType(classLoader.loadClass(getElementText(reader)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load field type", e);
                    }
                    break;
                }
                case FIELD_NAME: {
                    metaData.setFieldName(getElementText(reader));
                    break;
                }
                case READ_ONLY: {
                    metaData.setReadOnly(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case READ_TIMEOUT: {
                    metaData.setReadTimeout(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case COLUMN_NAME: {
                    metaData.setColumnName(getElementText(reader));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                case AUTO_INCREMENT: {
                    metaData.setAutoIncrement(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case KEY_GENERATOR_FACTORY: {
                    getElementText(reader); // TODO: Is this used?
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static JDBCEntityCommandMetaData parseEntityCommand(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {

        final JDBCEntityCommandMetaData metaData = new JDBCEntityCommandMetaData();

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    metaData.setName(reader.getAttributeValue(i));
                    break;
                }
                case CLASS: {
                    try {
                        metaData.setClass(classLoader.loadClass(reader.getAttributeValue(i)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load entity command class", e);
                    }

                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }

        for (Element element : children(reader)) {
            switch (element) {
                case ATTRIBUTE: {
                    parseAttribute(reader, metaData);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static void parseAttribute(final XMLStreamReader reader, JDBCEntityCommandMetaData metaData) throws XMLStreamException {

        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = reader.getAttributeValue(i);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        final String value = getElementText(reader);
        if (name != null) {
            metaData.addAttribute(name, value);
        }
    }

    private static void parseEntityCommands(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {

        for (Element element : children(reader)) {
            switch (element) {
                case ENTITY_COMMAND: {
                    applicationMetaData.addEntityCommand(parseEntityCommand(reader, applicationMetaData.getClassLoader()));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseRelationships(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {
        for (Element element : children(reader)) {
            switch (element) {
                case EJB_RELATION: {
                    applicationMetaData.addRelationship(parseRelationship(reader, applicationMetaData));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }


    private static JDBCRelationMetaData parseRelationship(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {
        final JDBCRelationMetaData metaData = new JDBCRelationMetaData();

        for (Element element : children(reader)) {
            switch (element) {
                case EJB_RELATION_NAME: {
                    metaData.setRelationName(getElementText(reader));
                    break;
                }
                case READ_ONLY: {
                    metaData.setReadOnly(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case READ_TIMEOUT: {
                    metaData.setReadTimeOut(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case FOREIGN_KEY_MAPPING: {
                    metaData.setMappingStyle(JDBCRelationMetaData.MappingStyle.FOREIGN_KEY);
                    break;
                }
                case RELATION_TABLE_MAPPING: {
                    metaData.setMappingStyle(JDBCRelationMetaData.MappingStyle.TABLE);
                    for (Element tableElement : children(reader)) {
                        switch (tableElement) {
                            case TABLE_NAME: {
                                metaData.setTableName(getElementText(reader));
                                break;
                            }
                            case DATASOURCE: {
                                metaData.setDataSourceName(getElementText(reader));
                                break;
                            }
                            case DATASOURCE_MAPPING: {
                                metaData.setDatasourceMapping(applicationMetaData.getTypeMappingByName(getElementText(reader)));
                                break;
                            }
                            case CREATE_TABLE: {
                                metaData.setCreateTable(Boolean.parseBoolean(getElementText(reader)));
                                break;
                            }
                            case REMOVE_TABLE: {
                                metaData.setRemoveTable(Boolean.parseBoolean(getElementText(reader)));
                                break;
                            }
                            case ALTER_TABLE: {
                                metaData.setAlterTable(Boolean.parseBoolean(getElementText(reader)));
                                break;
                            }
                            case POST_TABLE_CREATE: {
                                for (String cmd : parsePostTableCreate(reader)) {
                                    metaData.addPostTableCreate(cmd);
                                }
                                break;
                            }
                            case ROW_LOCKING: {
                                metaData.setRowLocking(Boolean.parseBoolean(getElementText(reader)));
                                break;
                            }
                            case PK_CONSTRAINT: {
                                metaData.setPrimaryKeyConstraint(Boolean.parseBoolean(getElementText(reader)));
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                    }
                    break;
                }
                case EJB_RELATIONSHIP_ROLE: {
                    metaData.addEjbRelationshipRole(parseEjbRelationshipRole(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        return metaData;
    }

    private static JDBCRelationshipRoleMetaData parseEjbRelationshipRole(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCRelationshipRoleMetaData metaData = new JDBCRelationshipRoleMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case EJB_RELATIONSHIP_ROLE_NAME: {
                    metaData.setRelationshipRoleName(getElementText(reader));
                    break;
                }
                case FK_CONSTRAINT: {
                    metaData.setForeignKeyConstraint(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case READ_AHEAD: {
                    metaData.setReadAhead(parseReadAhead(reader));
                    break;
                }
                case KEY_FIELDS: {
                    parseKeyFields(reader, metaData);
                    break;
                }
                case BATCH_CASCADE_DELETE: {
                    metaData.setBatchCascadeDelete(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static void parseKeyFields(final XMLStreamReader reader, final JDBCRelationshipRoleMetaData metaData) throws XMLStreamException {

        for (Element element : children(reader)) {
            switch (element) {
                case KEY_FIELD: {
                    metaData.addKeyField(parseCmpField(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }


    private static JDBCCMPFieldPropertyMetaData parseProperty(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCCMPFieldPropertyMetaData metaData = new JDBCCMPFieldPropertyMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case PROPERTY_NAME: {
                    metaData.setPropertyName(getElementText(reader));
                    break;
                }
                case COLUMN_NAME: {
                    metaData.setColumnName(getElementText(reader));
                    break;
                }
                case NOT_NULL: {
                    metaData.setNotNul(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static List<String> parsePostTableCreate(XMLStreamReader reader) throws XMLStreamException {
        final List<String> statements = new ArrayList<String>();
        for (Element element : children(reader)) {
            switch (element) {
                case SQL_STATEMENT: {
                    statements.add(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return statements;
    }

    private static void parsePostTableCreate(XMLStreamReader reader, final JDBCEntityMetaData entityMetaData) throws XMLStreamException {
        for (Element element : children(reader)) {
            switch (element) {
                case SQL_STATEMENT: {
                    entityMetaData.addPostTableCreate(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void parseEnterpriseBeans(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {
        for (Element element : children(reader)) {
            switch (element) {
                case ENTITY: {
                    applicationMetaData.addEntity(parseEntity(reader, applicationMetaData));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static JDBCEntityMetaData parseEntity(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {
        final JDBCEntityMetaData metaData = new JDBCEntityMetaData(applicationMetaData);
        final List<TempQueryMetaData> queries = new ArrayList<TempQueryMetaData>();
        final ClassLoader classLoader = applicationMetaData.getClassLoader();
        for (Element element : children(reader)) {
            switch (element) {
                case EJB_NAME: {
                    metaData.setEntityName(getElementText(reader));
                    break;
                }
                case DATASOURCE: {
                    metaData.setDataSource(getElementText(reader));
                    break;
                }
                case DATASOURCE_MAPPING: {
                    metaData.setDataSourceMapping(getElementText(reader));
                    break;
                }
                case CREATE_TABLE: {
                    metaData.setCreateTable(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case REMOVE_TABLE: {
                    metaData.setRemoveTable(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case POST_TABLE_CREATE: {
                    for (String cmd : parsePostTableCreate(reader)) {
                        metaData.addPostTableCreate(cmd);
                    }
                    break;
                }
                case READ_ONLY: {
                    metaData.setReadOnly(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case READ_TIMEOUT: {
                    metaData.setReadTimeout(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case ROW_LOCKING: {
                    metaData.setRowLocking(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case PK_CONSTRAINT: {
                    metaData.setPkConstraint(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case READ_AHEAD: {
                    metaData.setReadAhead(parseReadAhead(reader));
                    break;
                }
                case LIST_CACHE_MAX: {
                    metaData.setListCacheMax(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case CLEAN_READ_AHEAD: {
                    metaData.setCleanReadAheadOnLoad(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case FETCH_SIZE: {
                    metaData.setFetchSize(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case TABLE_NAME: {
                    metaData.setTableName(getElementText(reader));
                    break;
                }
                case CMP_FIELD: {
                    metaData.addCmpField(parseCmpField(reader));
                    break;
                }
                case LOAD_GROUPS: {
                    for (Map.Entry<String, List<String>> group : parseLoadGroups(reader).entrySet()) {
                        metaData.addLoadGroup(group.getKey(), group.getValue());
                    }
                    break;
                }
                case EAGER_LOAD_GROUP: {
                    metaData.setEagerLoadGroup(getElementText(reader));
                    break;
                }
                case LAZY_LOAD_GROUPS: {
                    metaData.addLazyLoadGroup(getElementText(reader));
                    break;
                }
                case QUERY: {
                    metaData.addTempQueryMetaData(parseQuery(reader, applicationMetaData));
                    break;
                }
                case UNKNOWN_PK: {
                    metaData.setUnknownPk(parseUnknownPk(reader, classLoader));
                    break;
                }
                case ENTITY_COMMAND: {
                    metaData.setEntityCommand(parseEntityCommand(reader, classLoader));
                    break;
                }
                case OPTIMISTIC_LOCKING: {
                    metaData.setOptimisticLocking(parseOptimisticLocking(reader, classLoader));
                    break;
                }
                case AUDIT: {
                    metaData.setAudit(parseAudit(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    public static class TempQueryMetaData {
        private String methodName;
        private List<String> methodParams = new ArrayList<String>();
        private String query;
        private Class<?> qlCompiler;
        private JDBCReadAheadMetaData readAheadMetaData;
        private JDBCQueryMetaDataFactory.Type type = JDBCQueryMetaDataFactory.Type.EJB_QL;
        private boolean lazyResultsetLoading;
        private Map<String, String> declaredParts = new HashMap<String, String>();

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public List<String> getMethodParams() {
            return methodParams;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public Class<?> getQlCompiler() {
            return qlCompiler;
        }

        public void setQlCompiler(Class<?> qlCompiler) {
            this.qlCompiler = qlCompiler;
        }

        public JDBCReadAheadMetaData getReadAheadMetaData() {
            return readAheadMetaData;
        }

        public void setReadAheadMetaData(JDBCReadAheadMetaData readAheadMetaData) {
            this.readAheadMetaData = readAheadMetaData;
        }

        public JDBCQueryMetaDataFactory.Type getType() {
            return type;
        }

        public void setType(JDBCQueryMetaDataFactory.Type type) {
            this.type = type;
        }

        public boolean isLazyResultsetLoading() {
            return lazyResultsetLoading;
        }

        public void setLazyResultsetLoading(boolean lazyResultsetLoading) {
            this.lazyResultsetLoading = lazyResultsetLoading;
        }


        public Map<String, String> getDeclaredParts() {
            return declaredParts;
        }
    }

    private static TempQueryMetaData parseQuery(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {
        final TempQueryMetaData metaData = new TempQueryMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case QUERY_METHOD: {
                    for (Element queryMethodChild : children(reader)) {
                        switch (queryMethodChild) {
                            case METHOD_NAME: {
                                metaData.setMethodName(getElementText(reader));
                                break;
                            }
                            case METHOD_PARAMS: {
                                for (Element paramChild : children(reader)) {
                                    switch (paramChild) {
                                        case METHOD_PARAM: {
                                            metaData.getMethodParams().add(getElementText(reader));
                                            break;
                                        }
                                        default: {
                                            throw unexpectedElement(reader);
                                        }
                                    }
                                }
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                    }
                    break;
                }
                case JBOSS_QL: {
                    metaData.setType(JDBCQueryMetaDataFactory.Type.JBOSS_QL);
                    metaData.setQuery(getElementText(reader));
                    break;
                }
                case DYNAMIC_QL: {
                    metaData.setType(JDBCQueryMetaDataFactory.Type.DYNAMIC_QL);
                    getElementText(reader);
                    break;
                }
                case DECLARED_QL: {
                    metaData.setType(JDBCQueryMetaDataFactory.Type.DECLARED_QL);
                    for (Element declaredChild : children(reader)) {
                        switch (declaredChild) {
                            case FROM: {
                                metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                break;
                            }
                            case WHERE: {
                                metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                break;
                            }
                            case ORDER: {
                                metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                break;
                            }
                            case OTHER: {
                                metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                break;
                            }
                            case SELECT: {
                                for (Element selectChild : children(reader)) {
                                    switch (selectChild) {
                                        case DISTINCT: {
                                            metaData.getDeclaredParts().put(element.getLocalName(), "");
                                            break;
                                        }
                                        case EJB_NAME: {
                                            metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                            break;
                                        }
                                        case FIELD_NAME: {
                                            metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                            break;
                                        }
                                        case ALIAS: {
                                            metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                            break;
                                        }
                                        case ADDITIONAL_COLUMNS: {
                                            metaData.getDeclaredParts().put(element.getLocalName(), getElementText(reader));
                                            break;
                                        }
                                        default: {
                                            throw unexpectedElement(reader);
                                        }
                                    }
                                }
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                    }
                    break;
                }
                case RAW_SQL: {
                    metaData.setType(JDBCQueryMetaDataFactory.Type.RAW_SQL);
                    metaData.setQuery(getElementText(reader));
                    break;
                }
                case READ_AHEAD: {
                    metaData.setReadAheadMetaData(parseReadAhead(reader));
                    break;
                }
                case QL_COMPILER: {
                    final String qlCompiler = getElementText(reader);
                    try {
                        metaData.setQlCompiler(applicationMetaData.getClassLoader().loadClass(qlCompiler));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load compiler implementation: " + qlCompiler, e);
                    }
                    break;
                }
                case LAZY_RESULTSET_LOADING: {
                    metaData.setLazyResultsetLoading(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static JDBCAuditMetaData parseAudit(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCAuditMetaData metaData = new JDBCAuditMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case CREATED_BY: {
                    metaData.setCreatedBy(parseCmpField(reader));
                    break;
                }
                case CREATED_TIME: {
                    metaData.setCreatedTime(parseCmpField(reader));
                    break;
                }
                case UPDATED_BY: {
                    metaData.setUpdatedBy(parseCmpField(reader));
                    break;
                }
                case UPDATED_TIME: {
                    metaData.setUpdatedTime(parseCmpField(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static JDBCOptimisticLockingMetaData parseOptimisticLocking(final XMLStreamReader reader, final ClassLoader classLoader) throws XMLStreamException {

        final JDBCOptimisticLockingMetaData metaData = new JDBCOptimisticLockingMetaData();
        final JDBCCMPFieldMetaData lockingField = new JDBCCMPFieldMetaData(null);
        for (Element element : children(reader)) {
            switch (element) {
                case GROUP_NAME: {
                    metaData.setGroupName(getElementText(reader));
                    break;
                }
                case MODIFIED_STRATEGY: {
                    metaData.setLockingStrategy(JDBCOptimisticLockingMetaData.LockingStrategy.MODIFIED_STRATEGY);
                    break;
                }
                case READ_STRATEGY: {
                    metaData.setLockingStrategy(JDBCOptimisticLockingMetaData.LockingStrategy.READ_STRATEGY);
                    break;
                }
                case VERSION_COLUMN: {
                    metaData.setLockingStrategy(JDBCOptimisticLockingMetaData.LockingStrategy.VERSION_COLUMN_STRATEGY);
                    break;
                }
                case TIMESTAMP_COLUMN: {
                    metaData.setLockingStrategy(JDBCOptimisticLockingMetaData.LockingStrategy.TIMESTAMP_COLUMN_STRATEGY);
                    break;
                }
                case KEY_GENERATOR_FACTORY: {
                    metaData.setLockingStrategy(JDBCOptimisticLockingMetaData.LockingStrategy.KEYGENERATOR_COLUMN_STRATEGY);
                    metaData.setKeyGeneratorFactory(getElementText(reader));
                    break;
                }
                case FIELD_TYPE: {
                    try {
                        lockingField.setFieldType(classLoader.loadClass(getElementText(reader)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load field type", e);
                    }
                    break;
                }
                case FIELD_NAME: {
                    lockingField.setFieldName(getElementText(reader));
                    break;
                }
                case COLUMN_NAME: {
                    lockingField.setColumnName(getElementText(reader));
                    break;
                }
                case JDBC_TYPE: {
                    lockingField.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    lockingField.setSqlType(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        metaData.setLockingField(lockingField);
        return metaData;
    }


    private static Map<String, List<String>> parseLoadGroups(final XMLStreamReader reader) throws XMLStreamException {
        final Map<String, List<String>> groups = new HashMap<String, List<String>>();
        for (Element element : children(reader)) {
            switch (element) {
                case LOAD_GROUP: {
                    parseLoadGroup(reader, groups);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return groups;
    }

    private static void parseLoadGroup(final XMLStreamReader reader, Map<String, List<String>> groups) throws XMLStreamException {
        String groupName = null;
        final List<String> fields = new ArrayList<String>();
        for (Element element : children(reader)) {
            switch (element) {
                case LOAD_GROUP_NAME: {
                    groupName = getElementText(reader);
                    break;
                }
                case FIELD_NAME: {
                    fields.add(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (groupName != null) {
            groups.put(groupName, fields);
        }
    }

    private static JDBCCMPFieldMetaData parseCmpField(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCCMPFieldMetaData metaData = new JDBCCMPFieldMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case COLUMN_NAME: {
                    metaData.setColumnName(getElementText(reader));
                    break;
                }
                case FIELD_NAME: {
                    metaData.setFieldName(getElementText(reader));
                    break;
                }
                case READ_ONLY: {
                    metaData.setReadOnly(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case READ_TIMEOUT: {
                    metaData.setReadTimeout(Integer.parseInt(getElementText(reader)));
                    break;
                }
                case NOT_NULL: {
                    metaData.setNotNull(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                case PROPERTY: {
                    metaData.addProperty(parseProperty(reader));
                    break;
                }
                case AUTO_INCREMENT: {
                    metaData.setAutoIncrement(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case DB_INDEX: {
                    metaData.setGenIndex(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case CHECK_DIRTY_AFTER_GET: {
                    metaData.setCheckDirtyAfterGet(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case STATE_FACTORY: {
                    metaData.setStateFactory(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static void parseDependentValueClasses(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {

        for (Element element : children(reader)) {
            switch (element) {
                case DEPENDENT_VALUE_CLASS: {
                    applicationMetaData.addValueClass(parseValueClass(reader, applicationMetaData));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static JDBCValueClassMetaData parseValueClass(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {

        final JDBCValueClassMetaData valueClass = new JDBCValueClassMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case CLASS: {
                    try {
                        valueClass.setClass(applicationMetaData.getClassLoader().loadClass(getElementText(reader)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Failed to load value class", e);
                    }

                    break;
                }
                case PROPERTY: {
                    valueClass.addProperty(parseValueProperty(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return valueClass;
    }

    private static JDBCValuePropertyMetaData parseValueProperty(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCValuePropertyMetaData metaData = new JDBCValuePropertyMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case PROPERTY_NAME: {
                    metaData.setPropertyName(getElementText(reader), null);
                    break;
                }
                case COLUMN_NAME: {
                    metaData.setColumnName(getElementText(reader));
                    break;
                }
                case NOT_NULL: {
                    metaData.setNotNul(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case JDBC_TYPE: {
                    metaData.setJdbcType(getJdbcTypeFromName(getElementText(reader)));
                    break;
                }
                case SQL_TYPE: {
                    metaData.setSqlType(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static List<JDBCUserTypeMappingMetaData> parseUserTypeMappings(final XMLStreamReader reader, final JDBCApplicationMetaData applicationMetaData) throws XMLStreamException {

        final List<JDBCUserTypeMappingMetaData> userTypeMappings = new ArrayList<JDBCUserTypeMappingMetaData>();
        for (Element element : children(reader)) {
            switch (element) {
                case USER_TYPE_MAPPING: {
                    userTypeMappings.add(parseUserTypeMapping(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return userTypeMappings;
    }

    private static JDBCUserTypeMappingMetaData parseUserTypeMapping(final XMLStreamReader reader) throws XMLStreamException {

        final JDBCUserTypeMappingMetaData metaData = new JDBCUserTypeMappingMetaData();
        for (Element element : children(reader)) {
            switch (element) {
                case JAVA_TYPE: {
                    metaData.setJavaType(getElementText(reader));
                    break;
                }
                case MAPPED_TYPE: {
                    metaData.setMappedType(getElementText(reader));
                    break;
                }
                case MAPPER: {
                    metaData.setMapper(getElementText(reader));
                    break;
                }
                case CHECK_DIRTY_AFTER_GET: {
                    metaData.setCheckDirtyAfterGet(Boolean.parseBoolean(getElementText(reader)));
                    break;
                }
                case STATE_FACTORY: {
                    metaData.setStateFactory(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return metaData;
    }

    private static void parseReservedWords(final XMLStreamReader reader) throws XMLStreamException {

        for (Element element : children(reader)) {
            switch (element) {
                case WORD: {
                    SQLUtil.addToRwords(getElementText(reader));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private static void moveToStart(final XMLStreamReader reader) throws XMLStreamException {
        reader.require(START_DOCUMENT, null, null);
        while (reader.hasNext() && reader.next() != START_ELEMENT) {
        }
    }

    private static boolean isEmpty(final String value) {
        return value == null || value.trim().equals("");
    }

    /**
     * Gets the JDBC type constant int for the name. The mapping from name to jdbc
     * type is contained in java.sql.Types.
     *
     * @param name the name for the jdbc type
     * @return the int type constant from java.sql.Types
     * @see java.sql.Types
     */
    public static int getJdbcTypeFromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("jdbc-type cannot be null");
        }

        try {
            return (Integer) Types.class.getField(name).get(null);
        } catch (Exception e) {
            return Types.OTHER;
        }
    }


    private static Iterable<Element> children(final XMLStreamReader reader) throws XMLStreamException {
        return new Iterable<Element>() {
            public Iterator<Element> iterator() {
                return new Iterator<Element>() {
                    public boolean hasNext() {
                        try {
                            return reader.hasNext() && reader.nextTag() != END_ELEMENT;
                        } catch (XMLStreamException e) {
                            throw new IllegalStateException("Unable to get next element: ", e);
                        }
                    }

                    public Element next() {
                        return Element.forName(reader.getLocalName());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Remove not supported");
                    }
                };
            }
        };
    }
}
