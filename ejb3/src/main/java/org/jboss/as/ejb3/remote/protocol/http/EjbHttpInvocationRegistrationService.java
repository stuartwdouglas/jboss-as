package org.jboss.as.ejb3.remote.protocol.http;

import io.undertow.util.Methods;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.remote.protocol.MarshallingSupport;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.httpinvocation.HttpInvocationRegistryService;
import org.wildfly.httpinvocation.MessageTypeSelectionHandler;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * @author Stuart Douglas
 */
public class EjbHttpInvocationRegistrationService implements Service<EjbHttpInvocationRegistrationService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "EjbHttpInvocationRegistrationService");

    public static final String EJB_EXCEPTION_V1 = "application/x-wf-ejb-exception; version=1";
    public static final String PATH = "/ejb";

    private InjectedValue<HttpInvocationRegistryService> httpInvocationRegistryServiceInjectedValue = new InjectedValue<>();
    private InjectedValue<DeploymentRepository> deploymentRepositoryInjectedValue = new InjectedValue<>();
    private InjectedValue<ExecutorService> executorInjectedValue = new InjectedValue<>();

    private final MarshallerFactory marshallerFactory;

    public EjbHttpInvocationRegistrationService(MarshallerFactory marshallerFactory) {
        this.marshallerFactory = marshallerFactory;
    }

    public static void install(ServiceTarget target, boolean executeInWorker, String threadPoolName) {
        EjbHttpInvocationRegistrationService service = new EjbHttpInvocationRegistrationService(Marshalling.getProvidedMarshallerFactory("river"));
        ServiceBuilder<EjbHttpInvocationRegistrationService> builder = target.addService(SERVICE_NAME, service)
                .addDependency(HttpInvocationRegistryService.SERVICE_NAME, HttpInvocationRegistryService.class, service.httpInvocationRegistryServiceInjectedValue)
                .addDependency(DeploymentRepository.SERVICE_NAME, DeploymentRepository.class, service.deploymentRepositoryInjectedValue)
                .setInitialMode(ServiceController.Mode.PASSIVE);
        if (!executeInWorker) {
            builder.addDependency(EJB3SubsystemModel.BASE_THREAD_POOL_SERVICE_NAME.append(threadPoolName), ExecutorService.class, service.executorInjectedValue);
        }
        builder.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        MessageTypeSelectionHandler selectionHandler = new MessageTypeSelectionHandler((out) -> {
            try {
                return MarshallingSupport.prepareForMarshalling(marshallerFactory, new DataOutputStream(out));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, EJB_EXCEPTION_V1);
        selectionHandler.register(HttpMethodInvocationMessageHandler.EJB_REQUEST, Methods.POST, new HttpMethodInvocationMessageHandler(deploymentRepositoryInjectedValue.getValue(), marshallerFactory, executorInjectedValue.getOptionalValue()));
        httpInvocationRegistryServiceInjectedValue.getValue().register(PATH, selectionHandler);
    }

    @Override
    public void stop(StopContext context) {
        httpInvocationRegistryServiceInjectedValue.getValue().unregister(PATH);
    }

    @Override
    public EjbHttpInvocationRegistrationService getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}
