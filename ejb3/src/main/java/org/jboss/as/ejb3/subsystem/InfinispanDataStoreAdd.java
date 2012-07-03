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

package org.jboss.as.ejb3.subsystem;

import java.util.List;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.persistence.infinispan.InfinispanTimerPersistence;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Adds the timer service file based data store
 *
 * @author Stuart Douglas
 */
public class InfinispanDataStoreAdd extends AbstractBoottimeAddStepHandler {

    public static final InfinispanDataStoreAdd INSTANCE = new InfinispanDataStoreAdd();

    protected void populateModel(ModelNode operation, ModelNode timerServiceModel) throws OperationFailedException {

        for (AttributeDefinition attr : InfinispanDataStoreResourceDefinition.ATTRIBUTES.values()) {
            attr.validateAndSet(operation, timerServiceModel);
        }
    }

    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final String cacheContainer = InfinispanDataStoreResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        final String cacheName = InfinispanDataStoreResourceDefinition.CACHE.resolveModelAttribute(context, model).asString();


        final InfinispanTimerPersistence infinispanTimerPersistence = new InfinispanTimerPersistence(cacheName);
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final ServiceName serviceName = TimerPersistence.SERVICE_NAME.append(address.getLastElement().getValue());
        newControllers.add(context.getServiceTarget().addService(serviceName, infinispanTimerPersistence)
                .addDependency(EmbeddedCacheManagerService.getServiceName(cacheContainer), EmbeddedCacheManager.class, infinispanTimerPersistence.getCacheContainerInjectedValue())
                .install());

    }

}
