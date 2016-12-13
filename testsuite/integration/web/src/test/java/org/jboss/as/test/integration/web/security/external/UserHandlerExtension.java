package org.jboss.as.test.integration.web.security.external;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

import javax.servlet.ServletContext;

/**
 * @author Stuart Douglas
 */
public class UserHandlerExtension implements ServletExtension {
    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        deploymentInfo.addInitialHandlerChainWrapper(handler -> new UserHandler(handler));
    }
}
