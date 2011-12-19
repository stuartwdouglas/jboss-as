package org.jboss.as.test.integration.web.async.weld;

import javax.enterprise.context.RequestScoped;

/**
 * 
 * 
 * @author Stuart Douglas
 */
@RequestScoped
public class RequestScopedObject {
    
    private final StringBuilder message = new StringBuilder();

    public void addMessage(String message) {
        this.message.append(message);
    }
    
    public String getMessage() {
        return message.toString();
    }
}
