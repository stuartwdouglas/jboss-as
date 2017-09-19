package org.wildfly.extension.undertow;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.undertow.server.HandlerWrapper;

/**
 * Handler wrapper service, that provides a global integration point for Undertow integration.
 *
 * These wrappers are installed before delegation to the per-host path handler
 */
public class HandlerWrapperService implements Service<HandlerWrapperService> {

    private final List<HandlerWrapper> handlerWrappers = new CopyOnWriteArrayList<>();
    private final List<Runnable> notifiers = new CopyOnWriteArrayList<>();

    public void addHandlerWrapper(HandlerWrapper handlerWrapper) {
        handlerWrappers.add(handlerWrapper);
        for (Runnable not : notifiers) {
            not.run();
        }
    }

    public void removeHandlerWrapper(HandlerWrapper handlerWrapper) {
        handlerWrappers.remove(handlerWrapper);
        for (Runnable not : notifiers) {
            not.run();
        }
    }

    public void addNotifier(Runnable runnable) {
        notifiers.add(runnable);
    }

    public void removeNotifier(Runnable runnable) {
        notifiers.remove(runnable);
    }

    public List<HandlerWrapper> getHandlerWrappers() {
        return Collections.unmodifiableList(handlerWrappers);
    }

    @Override
    public void start(StartContext startContext) throws StartException {

    }

    @Override
    public void stop(StopContext stopContext) {

    }

    @Override
    public HandlerWrapperService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
