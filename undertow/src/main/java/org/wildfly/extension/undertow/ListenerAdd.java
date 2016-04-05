/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.server.handlers.PeerNameResolvingHandler;
import io.undertow.servlet.handlers.MarkSecureHandler;
import io.undertow.util.HttpString;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;
import org.wildfly.extension.io.OptionList;
import org.wildfly.extension.undertow.deployment.GateHandlerWrapper;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.XnioWorker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
abstract class ListenerAdd extends AbstractAddStepHandler {

    ListenerAdd(ListenerResourceDefinition definition) {
        super(ListenerResourceDefinition.LISTENER_CAPABILITY, definition.getAttributes());
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = context.getCurrentAddress();
        final PathAddress parent = address.getParent();
        final String name = context.getCurrentAddressValue();
        final String bindingRef = ListenerResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        final String workerName = ListenerResourceDefinition.WORKER.resolveModelAttribute(context, model).asString();
        final String bufferPoolName = ListenerResourceDefinition.BUFFER_POOL.resolveModelAttribute(context, model).asString();
        final boolean enabled = ListenerResourceDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
        final boolean peerHostLookup = ListenerResourceDefinition.RESOLVE_PEER_ADDRESS.resolveModelAttribute(context, model).asBoolean();
        final boolean secure = ListenerResourceDefinition.SECURE.resolveModelAttribute(context, model).asBoolean();

        OptionMap listenerOptions = OptionList.resolveOptions(context, model, ListenerResourceDefinition.LISTENER_OPTIONS);
        OptionMap socketOptions = OptionList.resolveOptions(context, model, ListenerResourceDefinition.SOCKET_OPTIONS);
        String serverName = parent.getLastElement().getValue();
        final ServiceName listenerServiceName = UndertowService.listenerName(name);
        GateHandlerWrapper gateHandlerWrapper = new GateHandlerWrapper();
        final ListenerService<? extends ListenerService> service = createService(name, serverName, context, model, listenerOptions,socketOptions, gateHandlerWrapper);
        if (peerHostLookup) {
            service.addWrapperHandler(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    return new PeerNameResolvingHandler(handler);
                }
            });
        }
        if(secure) {
            service.addWrapperHandler(MarkSecureHandler.WRAPPER);
        }
        List<String> disallowedMethods = ListenerResourceDefinition.DISALLOWED_METHODS.unwrap(context, model);
        if(!disallowedMethods.isEmpty()) {
            final Set<HttpString> methodSet = new HashSet<>();
            for(String i : disallowedMethods) {
                methodSet.add(new HttpString(i.trim()));
            }
            service.addWrapperHandler(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    return new DisallowedMethodsHandler(handler, methodSet);
                }
            });
        }

        final ServiceName socketBindingServiceName = context.getCapabilityServiceName(ListenerResourceDefinition.SOCKET_CAPABILITY, bindingRef, SocketBinding.class);
        final ServiceName workerServiceName = context.getCapabilityServiceName(ListenerResourceDefinition.IO_WORKER_CAPABILITY, workerName, XnioWorker.class);
        final ServiceName bufferPoolServiceName = context.getCapabilityServiceName(ListenerResourceDefinition.IO_BUFFER_POOL_CAPABILITY, bufferPoolName, Pool.class);
        final ServiceBuilder<? extends ListenerService> serviceBuilder = context.getServiceTarget().addService(listenerServiceName, service);
        serviceBuilder.addDependency(workerServiceName, XnioWorker.class, service.getWorker())
                .addDependency(socketBindingServiceName, SocketBinding.class, service.getBinding())
                .addDependency(bufferPoolServiceName, (Injector) service.getBufferPool())
                .addDependency(UndertowService.SERVER.append(serverName), Server.class, service.getServerService());

        configureAdditionalDependencies(context, serviceBuilder, model, service);
        final ServiceController<? extends ListenerService> controller = serviceBuilder.setInitialMode(enabled ? ServiceController.Mode.ACTIVE : ServiceController.Mode.NEVER)
                .install();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final StabilityMonitor monitor = new StabilityMonitor();
                ServiceContainer serviceContainer = controller.getServiceContainer();
                serviceContainer.addMonitor(monitor);
                controller.addListener(new AbstractServiceListener<Object>() {
                    @Override
                    public void listenerAdded(ServiceController<?> controller) {
                        for(ServiceName name : controller.getServiceContainer().getServiceNames()) {
                            ServiceController<?> sc = controller.getServiceContainer().getService(name);
                            monitor.addController(sc);
                        }
                        monitor.addController(controller);
                        controller.removeListener(this);
                    }
                });

                ServiceController<?> execService = serviceContainer.getService(Services.JBOSS_SERVER_EXECUTOR);
                if(execService != null) {
                    ExecutorService executor = (ExecutorService) execService.getValue();
                    //this may be null in tests
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                monitor.awaitStability();
                                gateHandlerWrapper.open();
                            } catch (InterruptedException e) {
                                gateHandlerWrapper.open();
                            }
                        }
                    });
                } else {
                    gateHandlerWrapper.open();
                }
            }
        }, OperationContext.Stage.VERIFY);

    }

    abstract ListenerService<? extends ListenerService> createService(String name, final String serverName, final OperationContext context, ModelNode model, OptionMap listenerOptions, OptionMap socketOptions, GateHandlerWrapper gateHandlerWrapper) throws OperationFailedException;

    abstract void configureAdditionalDependencies(OperationContext context, ServiceBuilder<? extends ListenerService> serviceBuilder, ModelNode model, ListenerService service) throws OperationFailedException;

}
