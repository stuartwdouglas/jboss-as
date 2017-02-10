/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.remote.AssociationService;
import org.jboss.as.ejb3.remote.http.EJB3RemoteHTTPService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import io.undertow.server.handlers.PathHandler;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for invoking EJB's over HTTP
 *
 * @author Stuart Douglas
 */
public class EJB3RemoteHttpResourceDefinition extends SimpleResourceDefinition {

    static final String EJB_REMOTE_HTTP_CAPABILITY_NAME = "org.wildfly.ejb3.remote-http";
    static final String UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME = "org.wildfly.undertow.http-invoker";
    static final RuntimeCapability<Void> EJB_REMOTE_HTTP = RuntimeCapability.Builder.of(EJB_REMOTE_HTTP_CAPABILITY_NAME, false, EJB3RemoteHTTPService.class)
            .addRequirements(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME).build();


    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();


    private static final Map<String, AttributeDefinition> ATTRIBUTES;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(THREAD_POOL_NAME.getName(), THREAD_POOL_NAME);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }


    public static final EJB3RemoteHttpResourceDefinition INSTANCE = new EJB3RemoteHttpResourceDefinition();

    private EJB3RemoteHttpResourceDefinition() {
        super(EJB3SubsystemModel.REMOTE_HTTP_SERVICE_PATH, EJB3Extension
                .getResourceDescriptionResolver(EJB3SubsystemModel.REMOTE_HTTP), new RemoteHttpAddHandler(), new ServiceRemoveStepHandler(new RemoteHttpAddHandler()) {
            @Override
            protected ServiceName serviceName(String name) {
                return EJB_REMOTE_HTTP.getCapabilityServiceName();
            }
        });
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            // TODO: Make this read-write attribute
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        super.registerCapabilities(resourceRegistration);
        resourceRegistration.registerCapability(EJB_REMOTE_HTTP);
    }

    static class RemoteHttpAddHandler extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            THREAD_POOL_NAME.validateAndSet(operation, model);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            EJB3RemoteHTTPService service = new EJB3RemoteHTTPService();

            String threadPoolName = null;
            ModelNode threadPool = THREAD_POOL_NAME.resolveModelAttribute(context, model);
            if (threadPool.isDefined()) {
                threadPoolName = threadPool.asString();
            }

            ServiceBuilder<EJB3RemoteHTTPService> builder = context.getServiceTarget().addService(EJB_REMOTE_HTTP.getCapabilityServiceName(EJB3RemoteHTTPService.class), service)
                    .addDependency(context.getCapabilityServiceName(UNDERTOW_HTTP_INVOKER_CAPABILITY_NAME, PathHandler.class), PathHandler.class, service.getPathHandlerInjectedValue())
                    .addDependency(TxnServices.JBOSS_TXN_LOCAL_TRANSACTION_CONTEXT, LocalTransactionContext.class, service.getLocalTransactionContextInjectedValue())
                    .addDependency(AssociationService.SERVICE_NAME, AssociationService.class, service.getAssociationServiceInjectedValue());
            if (threadPoolName != null) {
                builder.addDependency(EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME.append(threadPoolName), ExecutorService.class, service.getExecutorServiceInjectedValue());
            }
            builder.install();
        }
    }


}
