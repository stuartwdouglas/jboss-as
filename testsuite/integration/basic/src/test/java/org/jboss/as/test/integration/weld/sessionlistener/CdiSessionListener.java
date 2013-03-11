package org.jboss.as.test.integration.weld.sessionlistener;

import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * @author Stuart Douglas
 */
@WebListener
public class CdiSessionListener implements HttpSessionListener {
    @Inject private SessionScopedBean bean;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        String sessionId = event.getSession().getId();
        bean.setSessionId(sessionId);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
    }
}