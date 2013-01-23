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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.as.jdkorb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * <p>
 * This class contains all JDK ORB subsystem attribute definitions.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class JDKORBSubsystemDefinitions {

    private static final ModelNode DEFAULT_DISABLED_PROPERTY = new ModelNode().set("off");

    private static final ModelNode DEFAULT_ENABLED_PROPERTY = new ModelNode().set("on");

    private static final ParameterValidator SSL_CONFIG_VALIDATOR =
            new EnumValidator<SSLConfigValue>(SSLConfigValue.class, true, false);

    private static final ParameterValidator ON_OFF_VALIDATOR = new EnumValidator<AllowedValues>(
            AllowedValues.class, true, false, AllowedValues.ON, AllowedValues.OFF);

    // orb attribute definitions.
    public static final SimpleAttributeDefinition ORB_NAME = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.NAME, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("JBoss"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_PRINT_VERSION = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_PRINT_VERSION, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_USE_IMR = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_USE_IMR, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_USE_BOM = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_USE_BOM, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CACHE_TYPECODES = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CACHE_TYPECODES, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CACHE_POA_NAMES = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CACHE_POA_NAMES, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_GIOP_MINOR_VERSION = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_GIOP_MINOR_VERSION, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(2))
            .setValidator(new IntRangeValidator(1, 2, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_SOCKET_BINDING, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jacorb"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_SSL_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_SSL_SOCKET_BINDING, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jacorb-ssl"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // connection attribute definitions.
    public static final SimpleAttributeDefinition ORB_CONN_RETRIES = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_RETRIES, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(5))
            .setValidator(new IntRangeValidator(0, 50, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_RETRY_INTERVAL = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_RETRY_INTERVAL, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(500))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_CLIENT_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_CLIENT_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(0))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_SERVER_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_SERVER_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(0))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_MAX_SERVER_CONNECTIONS = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_MAX_SERVER_CONNECTIONS, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(Integer.MAX_VALUE))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_MAX_MANAGED_BUF_SIZE = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_MAX_MANAGED_BUF_SIZE, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(24))
            .setValidator(new IntRangeValidator(0, 64, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_OUTBUF_SIZE = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_OUTBUF_SIZE, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(2048))
            .setValidator(new IntRangeValidator(0, 65536, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_OUTBUF_CACHE_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_CONN_OUTBUF_CACHE_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(-1))
            .setValidator(new IntRangeValidator(-1, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // initializers attribute definitions.
    public static final SimpleAttributeDefinition ORB_INIT_SECURITY = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_INIT_SECURITY, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_INIT_TX = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.ORB_INIT_TRANSACTIONS, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(new EnumValidator<AllowedValues>(AllowedValues.class, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // poa attribute definitions.
    public static final SimpleAttributeDefinition POA_MONITORING = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.POA_MONITORING, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_QUEUE_WAIT = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.POA_QUEUE_WAIT, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_QUEUE_MIN = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.POA_QUEUE_MIN, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(10))
            .setValidator(new IntRangeValidator(1, 100, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_QUEUE_MAX = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.POA_QUEUE_MAX, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(100))
            .setValidator(new IntRangeValidator(1, 200, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // request processor attribute definitions.
    public static final SimpleAttributeDefinition POA_REQUEST_PROC_POOL_SIZE = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.POA_RP_POOL_SIZE, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(5))
            .setValidator(new IntRangeValidator(1, 100, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_REQUEST_PROC_MAX_THREADS = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.POA_RP_MAX_THREADS, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(32))
            .setValidator(new IntRangeValidator(5, 150, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // naming attribute definitions.
    public static final SimpleAttributeDefinition NAMING_ROOT_CONTEXT = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.NAMING_ROOT_CONTEXT, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("JBoss/Naming/root"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition NAMING_EXPORT_CORBALOC = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.NAMING_EXPORT_CORBALOC, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // interoperability attribute definitions.
    public static final SimpleAttributeDefinition INTEROP_SUN = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.INTEROP_SUN, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_COMET = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.INTEROP_COMET, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_IONA = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.INTEROP_IONA, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_CHUNK_RMI_VALUETYPES = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.INTEROP_CHUNK_RMI_VALUETYPES, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_LAX_BOOLEAN_ENCODING = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.INTEROP_LAX_BOOLEAN_ENCODING, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_INDIRECT_ENCODING_DISABLE = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.INTEROP_INDIRECTION_ENCODING_DISABLE, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_STRICT_CHECK_ON_TC_CREATION = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.INTEROP_STRICT_CHECK_ON_TC_CREATION, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // security attribute definitions.
    public static final SimpleAttributeDefinition SECURITY_SUPPORT_SSL = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.SECURITY_SUPPORT_SSL, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.SECURITY_SECURITY_DOMAIN, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_ADD_COMPONENT_INTERCEPTOR = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setValidator(ON_OFF_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_CLIENT_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.SECURITY_CLIENT_SUPPORTS, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_CLIENT_REQUIRES = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.SECURITY_CLIENT_REQUIRES, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SERVER_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.SECURITY_SERVER_SUPPORTS, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.MUTUALAUTH.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SERVER_REQUIRES = new SimpleAttributeDefinitionBuilder(
            JDKORBSubsystemConstants.SECURITY_SERVER_REQUIRES, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set(SSLConfigValue.NONE.toString()))
            .setValidator(SSL_CONFIG_VALIDATOR)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final PropertiesAttributeDefinition PROPERTIES =
            new PropertiesAttributeDefinition(JDKORBSubsystemConstants.PROPERTIES,
                    JDKORBSubsystemConstants.PROPERTIES, true);


    // list that contains the orb attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_ATTRIBUTES = Arrays.asList(ORB_NAME, ORB_PRINT_VERSION,
            ORB_USE_IMR, ORB_USE_BOM, ORB_CACHE_TYPECODES, ORB_CACHE_POA_NAMES, ORB_GIOP_MINOR_VERSION,
            ORB_SOCKET_BINDING, ORB_SSL_SOCKET_BINDING);

    // list that contains the orb connection attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_CONN_ATTRIBUTES = Arrays.asList(ORB_CONN_RETRIES,
            ORB_CONN_RETRY_INTERVAL, ORB_CONN_CLIENT_TIMEOUT, ORB_CONN_SERVER_TIMEOUT, ORB_CONN_MAX_SERVER_CONNECTIONS,
            ORB_CONN_MAX_MANAGED_BUF_SIZE, ORB_CONN_OUTBUF_SIZE, ORB_CONN_OUTBUF_CACHE_TIMEOUT);

    // list that contains the orb initializer attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_INIT_ATTRIBUTES = Arrays.asList(ORB_INIT_SECURITY, ORB_INIT_TX);

    // list that contains the poa attribute definitions.
    static final List<SimpleAttributeDefinition> POA_ATTRIBUTES = Arrays.asList(POA_MONITORING, POA_QUEUE_WAIT,
            POA_QUEUE_MIN, POA_QUEUE_MAX);

    // list that contains the poa request processor attribute definitions.
    static final List<SimpleAttributeDefinition> POA_RP_ATTRIBUTES = Arrays.asList(POA_REQUEST_PROC_POOL_SIZE,
            POA_REQUEST_PROC_MAX_THREADS);

    // list that contains the naming attribute definitions.
    static final List<SimpleAttributeDefinition> NAMING_ATTRIBUTES = Arrays.asList(NAMING_ROOT_CONTEXT,
            NAMING_EXPORT_CORBALOC);

    // list that contains the interoperability attribute definitions.
    static final List<SimpleAttributeDefinition> INTEROP_ATTRIBUTES = Arrays.asList(INTEROP_SUN, INTEROP_COMET,
            INTEROP_IONA, INTEROP_CHUNK_RMI_VALUETYPES, INTEROP_LAX_BOOLEAN_ENCODING, INTEROP_INDIRECT_ENCODING_DISABLE,
            INTEROP_STRICT_CHECK_ON_TC_CREATION);

    // list that contains the security attribute definitions.
    static final List<SimpleAttributeDefinition> SECURITY_ATTRIBUTES = Arrays.asList(SECURITY_SUPPORT_SSL,
            SECURITY_SECURITY_DOMAIN, SECURITY_ADD_COMPONENT_INTERCEPTOR, SECURITY_CLIENT_SUPPORTS,
            SECURITY_CLIENT_REQUIRES, SECURITY_SERVER_SUPPORTS, SECURITY_SERVER_REQUIRES);

    static final List<SimpleAttributeDefinition> SSL_CONFIG_ATTRIBUTES = Arrays.asList(SECURITY_CLIENT_SUPPORTS,
            SECURITY_CLIENT_REQUIRES, SECURITY_SERVER_SUPPORTS, SECURITY_SERVER_REQUIRES);

    // list that contains all attribute definitions.
    static final List<AttributeDefinition> SUBSYSTEM_ATTRIBUTES;

    // utility map that keys all definitions by their names.
    static final Map<String, AttributeDefinition> ATTRIBUTES_BY_NAME;

    static {
        SUBSYSTEM_ATTRIBUTES = new ArrayList<AttributeDefinition>();
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_CONN_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_INIT_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(POA_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(POA_RP_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(NAMING_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(INTEROP_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(SECURITY_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.add(PROPERTIES);

        Map<String, AttributeDefinition> map = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition attribute : SUBSYSTEM_ATTRIBUTES) {
            map.put(attribute.getName(), attribute);
        }
        ATTRIBUTES_BY_NAME = map;
    }

    /**
     * <p>
     * Gets the {@code SimpleAttributeDefinition} identified by the specified name.
     * </p>
     *
     * @param attributeNAme a {@code String} representing the attribute name.
     * @return the corresponding attribute definition or {@code null} if no definition was found with that name.
     */
    public static AttributeDefinition valueOf(String attributeNAme) {
        return ATTRIBUTES_BY_NAME.get(attributeNAme);
    }

}