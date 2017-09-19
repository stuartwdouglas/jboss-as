package org.wildfly.extension.grpc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jboss.as.ee.component.ComponentRegistry;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.invocation.proxy.reflection.ClassMetadataSource;
import org.jboss.invocation.proxy.reflection.DefaultReflectionMetadataSource;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.HandlerWrapperService;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerServiceDefinition;
import io.undertow.grpc.UndertowServerBuilder;
import io.undertow.server.HandlerWrapper;

class GrpcDeploymentService implements Service<GrpcDeploymentService> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("grpc-deployment-service");

    private final List<Class<?>> grpcClasses;
    private final InjectedValue<HandlerWrapperService> handlerWrapperService = new InjectedValue<>();
    private HandlerWrapper wrapper;
    private final InjectedValue<ComponentRegistry> componentRegistryInjectedValue = new InjectedValue<>();

    public GrpcDeploymentService(List<Class<?>> grpcClasses) {
        this.grpcClasses = grpcClasses;
    }

    @Override
    public void start(StartContext startContext) throws StartException {

        UndertowServerBuilder serverBuilder = new UndertowServerBuilder();
        try {
            for (Class<?> realClass : grpcClasses) {
                try {
                    //first we need to look for the abstract GRPC superclass
                    boolean ignore = false;
                    Class<?> cl = realClass;
                    for (; ; ) {
                        if (cl == null || cl == Object.class) {
                            ignore = true;
                            break;
                        }
                        if (Modifier.isAbstract(cl.getModifiers())) {
                            Method bindService = cl.getDeclaredMethod("bindService");
                            if (bindService != null && bindService.getReturnType() == ServerServiceDefinition.class) {
                                //we have found or base, basically an abstract class that defines the bindService method
                                break;
                            }
                        }
                        cl = cl.getSuperclass();
                    }
                    if (ignore) {
                        GrpcLogger.ROOT_LOGGER.unableToProcess(realClass);
                        continue;
                    }

                    ProxyConfiguration<BindableService> objectProxyConfiguration = new ProxyConfiguration<>();
                    objectProxyConfiguration.setClassLoader(realClass.getClassLoader());
                    objectProxyConfiguration.setSuperClass((Class<BindableService>) realClass);
                    objectProxyConfiguration.setProxyName(realClass.getName() + "$$grpc$registration$proxy");
                    objectProxyConfiguration.setMetadataSource(clazz -> {
                        ClassMetadataSource metadata = DefaultReflectionMetadataSource.INSTANCE.getClassMetadata(clazz);
                        return new ClassMetadataSource() {
                            @Override
                            public Collection<Method> getDeclaredMethods() {
                                Collection<Method> existing = new HashSet<>(metadata.getDeclaredMethods());
                                existing.removeIf(method -> method.getName().equals("bindService") && method.getReturnType() == ServerServiceDefinition.class);
                                return existing;
                            }

                            @Override
                            public Method getMethod(String methodName, Class<?> returnType, Class<?>... parameters) throws NoSuchMethodException {
                                if (methodName.equals("bindService") && returnType == ServerServiceDefinition.class && parameters.length == 0) {
                                    return null;
                                }
                                return metadata.getMethod(methodName, returnType, parameters);
                            }

                            @Override
                            public Collection<Constructor<?>> getConstructors() {
                                return metadata.getConstructors();
                            }
                        };
                    });
                    Object createdInstance = componentRegistryInjectedValue.getValue().createInstanceFactory(realClass).getReference().getInstance();
                    ProxyFactory<BindableService> factory = new ProxyFactory<>(objectProxyConfiguration);
                    BindableService service = factory.newInstance(new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return method.invoke(createdInstance, args);
                        }
                    });
                    serverBuilder.addService(service.bindService());
                } catch (Exception e) {
                    GrpcLogger.ROOT_LOGGER.failedToInstall(realClass, e);
                }
            }

            Server server = serverBuilder.build();
            server.start();
            wrapper = serverBuilder.getHandlerWrapper();
            handlerWrapperService.getValue().addHandlerWrapper(wrapper);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        handlerWrapperService.getValue().removeHandlerWrapper(wrapper);
    }

    @Override
    public GrpcDeploymentService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    InjectedValue<HandlerWrapperService> getHandlerWrapperService() {
        return handlerWrapperService;
    }

    InjectedValue<ComponentRegistry> getComponentRegistryInjectedValue() {
        return componentRegistryInjectedValue;
    }
}
