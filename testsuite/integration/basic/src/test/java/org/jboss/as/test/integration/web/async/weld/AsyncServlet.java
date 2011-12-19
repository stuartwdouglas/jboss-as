/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.web.async.weld;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 */
@WebServlet(name = "AsyncServlet", urlPatterns = { "/async/" }, loadOnStartup = 1, asyncSupported = true)
public class AsyncServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String SYNC_PART = "Sync Part";
    public static final String ASYNC_PART = "ASync Part";

    @Inject
    private RequestScopedObject requestScopedObject;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final AsyncContext aCtx = request.startAsync(request, response);
        final RequestScopedObject requestScopedObject = this.requestScopedObject;
        requestScopedObject.addMessage(SYNC_PART);
        
        Thread t = new Thread(new Runnable(){
            @Override
            public void run() {
                aCtx.start(new Runnable() {
                    @Override
                    public void run() {
                        requestScopedObject.addMessage(ASYNC_PART);
                        try {
                            aCtx.getResponse().getWriter().append(requestScopedObject.getMessage());
                            aCtx.getResponse().getWriter().close();
                            aCtx.complete();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
        
                    }
                });
            }
        });
        t.start();
    }
}
