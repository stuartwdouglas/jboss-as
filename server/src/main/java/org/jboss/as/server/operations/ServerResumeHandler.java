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
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.controller.descriptions.ServerRootDescription;
import org.jboss.as.server.suspend.SuspendManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler that resumes the standalone server.
 *
 * @author Stuart Douglas
 */
public class ServerResumeHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "resume";
    public static final ServerResumeHandler INSTANCE = new ServerResumeHandler();

    private ServerResumeHandler() {
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
                manager.resume();

                //TODO: Is the the correct behaviour
                if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                    final ServiceVerificationHandler resumeHandler = new ServiceVerificationHandler();
                    manager.suspend();
                    context.addStep(resumeHandler, OperationContext.Stage.VERIFY);
                }
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    /**
     * {@inheritDoc}
     */
    public ModelNode getModelDescription(final Locale locale) {
        return ServerRootDescription.getResumeOperationDescription(locale);
    }
}
