package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author Stuart Douglas
 */
public class DeploymentRepositoryProcessor implements DeploymentUnitProcessor {

    final DeploymentRepository deploymentRepository;

    public DeploymentRepositoryProcessor(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (eeModuleDescription == null) {
            return;
        }
        final DeploymentModuleIdentifier identifier = new DeploymentModuleIdentifier(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getDistinctName());


    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        if (eeModuleDescription == null) {
            return;
        }
        final DeploymentModuleIdentifier identifier = new DeploymentModuleIdentifier(eeModuleDescription.getApplicationName(), eeModuleDescription.getModuleName(), eeModuleDescription.getDistinctName());
        deploymentRepository.remove(identifier);
    }
}
