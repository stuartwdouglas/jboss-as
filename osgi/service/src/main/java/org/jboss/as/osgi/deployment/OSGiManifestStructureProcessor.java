/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.deployment;

import java.util.jar.Manifest;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.spi.OSGiManifestBuilder;

/**
 * Processes deployments that contain a valid OSGi manifest.
 *
 * If so it attaches the {@link Manifest} under key {@link OSGIAttachments#OSGI_MANIFEST}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Dec-2010
 */
public class OSGiManifestStructureProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        if (depUnit.hasAttachment(OSGIAttachments.OSGI_MANIFEST) || depUnit.hasAttachment(OSGIAttachments.OSGI_METADATA))
            return;

        final ResourceRoot deploymentRoot = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (deploymentRoot == null)
            return;

        // Check whether this is an OSGi manifest
        Manifest manifest = deploymentRoot.getAttachment(Attachments.MANIFEST);
        if (OSGiManifestBuilder.isValidBundleManifest(manifest)) {
            depUnit.putAttachment(Attachments.OSGI_DEPLOYMENT, true);
            depUnit.putAttachment(OSGIAttachments.OSGI_MANIFEST, manifest);
            OSGiMetaData metadata = OSGiMetaDataBuilder.load(manifest);
            depUnit.putAttachment(OSGIAttachments.OSGI_METADATA, metadata);

            // JAR deployments may contain OSGi metadata with a "Web-ContextPath" header
            // This qualifies them as OSGi Web Application Bundle (WAB)
            if (depUnit.getName().endsWith(".jar")) {
                if (metadata.getHeader("Web-ContextPath") != null) {
                    DeploymentTypeMarker.setType(DeploymentType.WAR, depUnit);
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.OSGI_DEPLOYMENT);
        deploymentUnit.removeAttachment(OSGIAttachments.OSGI_MANIFEST);
        deploymentUnit.removeAttachment(OSGIAttachments.OSGI_METADATA);
    }
}
