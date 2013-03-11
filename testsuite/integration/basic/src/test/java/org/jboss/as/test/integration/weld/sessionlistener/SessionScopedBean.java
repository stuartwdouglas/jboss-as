package org.jboss.as.test.integration.weld.sessionlistener;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

/**
 * @author Stuart Douglas
 */
@SessionScoped
public class SessionScopedBean implements Serializable {

    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }
}
