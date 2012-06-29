package org.jboss.as.sunorb;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Tomaz Cerar
 * @created 6.1.12 23:00
 */
public class SunORBSubsystemResource extends SimpleResourceDefinition {
    public static final SunORBSubsystemResource INSTANCE = new SunORBSubsystemResource();
    private static final ReloadRequiredWriteAttributeHandler ATTRIBUTE_HANDLER = new ReloadRequiredWriteAttributeHandler();

    private SunORBSubsystemResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SunORBExtension.SUBSYSTEM_NAME),
                SunORBExtension.getResourceDescriptionResolver(SunORBExtension.SUBSYSTEM_NAME),
                SunORBSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }


    @Override
    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : SunORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, ATTRIBUTE_HANDLER);
        }

    }
}
