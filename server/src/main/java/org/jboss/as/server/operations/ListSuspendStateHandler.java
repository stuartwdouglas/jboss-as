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

package org.jboss.as.server.operations;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.server.controller.descriptions.ServerRootDescription;
import org.jboss.as.server.suspend.SuspendManager;
import org.jboss.as.server.suspend.SuspendPermitManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler that suspends the standalone server.
 *
 * @author Stuart Douglas
 */
public class ListSuspendStateHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = ModelDescriptionConstants.LIST_SUSPEND_STATE;
    public static final ListSuspendStateHandler INSTANCE = new ListSuspendStateHandler();

    private ListSuspendStateHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final ServiceController<?> service = context.getServiceRegistry(true).getRequiredService(SuspendManager.SERVICE_NAME);
                SuspendManager manager = (SuspendManager) service.getValue();
                final ModelNode result = new ModelNode();
                result.setEmptyList();
                for(final SuspendPermitManager p : manager.getPermitManagers()) {
                    ModelNode sp = new ModelNode();
                    sp.get(ModelDescriptionConstants.NAME).set(p.getName());
                    sp.get(ModelDescriptionConstants.OUTSTANDING_PERMITS).set(p.getOutstandingPermits());
                    result.add(sp);
                }


                context.getResult().set(result);
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    /**
     * {@inheritDoc}
     */
    public ModelNode getModelDescription(final Locale locale) {
        return ServerRootDescription.getSuspendOperationDescription(locale);
    }
}
