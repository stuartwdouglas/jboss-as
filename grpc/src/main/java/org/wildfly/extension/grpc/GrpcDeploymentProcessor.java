package org.wildfly.extension.grpc;

import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_HANDLER_WRAPPER;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.wildfly.extension.undertow.HandlerWrapperService;

import io.grpc.BindableService;

public class GrpcDeploymentProcessor implements DeploymentUnitProcessor {

    static final RuntimeCapability<Void> HANDLER_WRAPPER_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(CAPABILITY_HANDLER_WRAPPER, false, HandlerWrapperService.class)
                    .build();

    public static final DotName BINDABLE_CLASS = DotName.createSimple(BindableService.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        Set<ClassInfo> grpcClasses = index.getAllKnownImplementors(BINDABLE_CLASS);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(module.getClassLoader());
            List<Class<?>> classList = new ArrayList<>();
            if (grpcClasses != null && !grpcClasses.isEmpty()) {

                for (ClassInfo clazz : grpcClasses) {
                    if ((clazz.flags() & Modifier.ABSTRACT) == 0) {
                        try {
                            final Class<?> realClass = Class.forName(clazz.name().toString(), true, module.getClassLoader());
                            classList.add(realClass);
                        } catch (Throwable t) {
                            GrpcLogger.ROOT_LOGGER.failedToLoad(clazz.name().toString(), t);
                        }
                    }
                }
            }

            GrpcDeploymentService service = new GrpcDeploymentService(classList);
            deploymentPhaseContext.getServiceTarget().addService(deploymentUnit.getServiceName().append(GrpcDeploymentService.SERVICE_NAME), service)
                    .addDependency(HANDLER_WRAPPER_RUNTIME_CAPABILITY.getCapabilityServiceName(), HandlerWrapperService.class, service.getHandlerWrapperService())
                    .addDependency(ComponentRegistry.serviceName(deploymentUnit), ComponentRegistry.class, service.getComponentRegistryInjectedValue())
                    .install();

        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}
