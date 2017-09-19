package org.wildfly.extension.grpc;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;

/**
 * @author Stuart Douglas
 */
class GrpcResourceDefinition extends PersistentResourceDefinition {

    static GrpcResourceDefinition INSTANCE = new GrpcResourceDefinition();

    private GrpcResourceDefinition() {
        super(
                GrpcExtension.PATH_SUBSYSTEM,
                GrpcExtension.getResourceDescriptionResolver(),
                GrpcSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }


    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }
}
