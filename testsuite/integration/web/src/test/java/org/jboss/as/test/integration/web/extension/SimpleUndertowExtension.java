package org.jboss.as.test.integration.web.extension;

import javax.servlet.ServletContext;

import io.undertow.io.IoCallback;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * @author Stuart Douglas
 */
public class SimpleUndertowExtension implements ServletExtension {

    static final String THIS_IS_NOT_A_SERVLET = "This is not a servlet";

    @Override
    public void handleDeployment(final DeploymentInfo deploymentInfo, final ServletContext servletContext) {
        deploymentInfo.addInitialHandlerChainWrapper(handler -> exchange -> {
            if(Thread.currentThread() != exchange.getIoThread()) {
                exchange.setStatusCode(500);
                exchange.getResponseSender().send("Response was dispatched, not running in IO thread", IoCallback.END_EXCHANGE);
            }
            exchange.getResponseSender().send(THIS_IS_NOT_A_SERVLET, IoCallback.END_EXCHANGE);
        });
    }
}
