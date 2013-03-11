package org.jboss.as.test.integration.weld.sessionlistener;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
@WebServlet(urlPatterns = "/*")
public class CdiSessionListenerServlet extends HttpServlet {

    @Inject
    private SessionScopedBean bean;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final String sessionID = req.getSession(true).getId();
        resp.getWriter().write("Session: " + sessionID + " EJB: " + bean.getSessionId());
    }
}
