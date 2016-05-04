package org.jboss.as.ejb3.remote.protocol.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.remote.protocol.MarshallingSupport;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.wildfly.httpinvocation.HttpMessageHandler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author Stuart Douglas
 */
public abstract class AbstractEjbHttpMessageHandler implements HttpMessageHandler {

    public static final String EXCEPTION_MESSAGE_TYPE = "application/x-wf-ejb-exception; version=1";

    protected static EjbDeploymentInformation findEJB(DeploymentRepository deploymentRepository, final String appName, final String moduleName, final String distinctName, final String beanName) {
        final DeploymentModuleIdentifier ejbModule = new DeploymentModuleIdentifier(appName, moduleName, distinctName);
        final Map<DeploymentModuleIdentifier, ModuleDeployment> modules = deploymentRepository.getStartedModules();
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        final ModuleDeployment moduleDeployment = modules.get(ejbModule);
        if (moduleDeployment == null) {
            return null;
        }
        return moduleDeployment.getEjbs().get(beanName);
    }

    protected void sendError(MarshallerFactory marshallerFactory, HttpServerExchange exchange, Throwable t, int status) throws IOException {
        exchange.setStatusCode(status);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, EXCEPTION_MESSAGE_TYPE);
        if(t != null) {
            try (DataOutputStream outputStream = new DataOutputStream(exchange.getOutputStream())) {
                // write out the exception
                final Marshaller marshaller = MarshallingSupport.prepareForMarshalling(marshallerFactory, outputStream);
                marshaller.writeObject(t);
                // finish marshalling
                marshaller.finish();
            }
        }
        exchange.endExchange();
    }

}
