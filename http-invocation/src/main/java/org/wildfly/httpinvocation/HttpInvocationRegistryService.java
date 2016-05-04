package org.wildfly.httpinvocation;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that handles registration of HTTP services
 *
 * @author Stuart Douglas
 */
public class HttpInvocationRegistryService implements Service<HttpInvocationRegistryService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("http", "invocation", "service");

    public static void install(ServiceTarget serviceTarget) {
        serviceTarget.addService(SERVICE_NAME, new HttpInvocationRegistryService())
                .install();
    }

    private final PathHandler pathHandler = new PathHandler();

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }

    public void register(final String path, HttpHandler handler) {
        pathHandler.addPrefixPath(path, handler);
    }

    public void unregister(final String path) {
        pathHandler.removePrefixPath(path);
    }

    public PathHandler getPathHandler() {
        return pathHandler;
    }

    @Override
    public HttpInvocationRegistryService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
