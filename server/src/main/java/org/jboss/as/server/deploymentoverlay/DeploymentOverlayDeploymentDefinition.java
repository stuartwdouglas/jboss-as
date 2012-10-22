package org.jboss.as.server.deploymentoverlay;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;

/**
 * Links a deployment overlay to a deployment
 * @author Stuart Douglas
 */
public class DeploymentOverlayDeploymentDefinition extends SimpleResourceDefinition {

    private static final AttributeDefinition[] ATTRIBUTES = { };

    public static AttributeDefinition[] attributes() {
        return ATTRIBUTES.clone();
    }

    public DeploymentOverlayDeploymentDefinition(DeploymentOverlayPriority priority) {
        super(DeploymentOverlayModel.DEPLOYMENT_OVERRIDE_DEPLOYMENT_PATH,
                ControllerResolver.getResolver(ModelDescriptionConstants.DEPLOYMENT_OVERLAY + "." + ModelDescriptionConstants.DEPLOYMENT),
                new DeploymentOverlayDeploymentAdd(priority),
                new DeploymentOverlayDeploymentRemove(priority));
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }
}
