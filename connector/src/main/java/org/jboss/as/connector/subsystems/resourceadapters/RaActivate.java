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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler responsible for disabling an existing data-source.
 *
 * @author Stefano Maestri
 */
public class RaActivate implements OperationStepHandler {
    static final RaActivate INSTANCE = new RaActivate();

    public void execute(OperationContext context, ModelNode operation)  throws OperationFailedException {

        final ModelNode address = operation.require(OP_ADDR);
        final String idName = PathAddress.pathAddress(address).getLastElement().getValue();
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        final String archiveOrModuleName;
        if (!model.hasDefined(ARCHIVE.getName()) && !model.hasDefined(MODULE.getName())) {
            throw ConnectorLogger.ROOT_LOGGER.archiveOrModuleRequired();
        }
        if (model.get(ARCHIVE.getName()).isDefined()) {
            archiveOrModuleName = model.get(ARCHIVE.getName()).asString();
        } else {
            archiveOrModuleName = model.get(MODULE.getName()).asString();
        }

        if (context.isNormalServer()) {
            context.addStep((context12, operation12) -> {

                ServiceName restartedServiceName = RaOperationUtil.restartIfPresent(context12, archiveOrModuleName, idName);

                if (restartedServiceName == null) {
                    RaOperationUtil.activate(context12, idName, archiveOrModuleName);
                }
                context12.completeStep((context1, operation1) -> {
                    try {
                        RaOperationUtil.removeIfActive(context1, archiveOrModuleName, idName);
                    } catch (OperationFailedException e) {

                    }

                });
            }, OperationContext.Stage.RUNTIME);
        }
        context.stepCompleted();
    }

}
