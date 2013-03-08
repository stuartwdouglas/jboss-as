/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.util;

import static org.jboss.as.webservices.WSMessages.MESSAGES;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.naming.NamingException;
import javax.servlet.Servlet;

import org.apache.catalina.LifecycleException;
import org.apache.tomcat.InstanceManager;
import org.jboss.as.web.host.CommonServletBuilder;
import org.jboss.as.web.host.CommonWebDeployment;
import org.jboss.as.web.host.CommonWebDeploymentBuilder;
import org.jboss.as.web.host.CommonWebHost;
import org.jboss.msc.service.StartException;

/**
 * WebAppController allows for automatically starting/stopping a webapp (servlet) depending on the actual need. This is useful
 * for WS deployments needing a given utility servlet to be up (for instance the port component link servlet)
 *
 * @author alessio.soldano@jboss.com
 * @since 02-Dec-2011
 */
public class WebAppController {

    private CommonWebHost host;
    private String contextRoot;
    private String urlPattern;
    private String serverTempDir;
    private String servletClass;
    private ClassLoader classloader;
    private volatile CommonWebDeployment ctx;
    private int count = 0;

    public WebAppController(CommonWebHost host, String servletClass, ClassLoader classloader, String contextRoot, String urlPattern,
            String serverTempDir) {
        this.host = host;
        this.contextRoot = contextRoot;
        this.urlPattern = urlPattern;
        this.serverTempDir = serverTempDir;
        this.classloader = classloader;
        this.servletClass = servletClass;
    }

    public synchronized int incrementUsers() throws StartException {
        if (count == 0) {
            try {
                ctx = startWebApp(host);
            } catch (Exception e) {
                throw new StartException(e);
            }
        }
        return count++;
    }

    public synchronized int decrementUsers() {
        if (count == 0) {
            throw new IllegalStateException();
        }
        count--;
        if (count == 0) {
            try {
                stopWebApp(ctx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return count;
    }

    private CommonWebDeployment startWebApp(CommonWebHost host) throws Exception {
        CommonWebDeploymentBuilder builder = new CommonWebDeploymentBuilder();
        CommonWebDeployment deployment;
        try {
            builder.setContextRoot(contextRoot);
            File docBase = new File(serverTempDir, contextRoot);
            if (!docBase.exists()) {
                docBase.mkdirs();
            }
            builder.setDocumentRoot(docBase);
            builder.setClassLoader(classloader);

            final int j = servletClass.indexOf(".");
            final String servletName = j < 0 ? servletClass : servletClass.substring(j + 1);
            final Class<?> clazz = classloader.loadClass(servletClass);
            CommonServletBuilder servlet = new CommonServletBuilder();
            servlet.setServletName(servletName);
            servlet.setServlet((Servlet) clazz.newInstance());
            servlet.setServletClass(clazz);
            servlet.getUrlMappings().add(urlPattern);
            builder.addServlet(servlet);

            deployment = host.addWebDeployment(builder);
            deployment.create();

        } catch (Exception e) {
            throw MESSAGES.createContextPhaseFailed(e);
        }
        try {
            deployment.start();
        } catch (LifecycleException e) {
            throw MESSAGES.startContextPhaseFailed(e);
        }
        return deployment;
    }

    private void stopWebApp(CommonWebDeployment context) throws Exception {
        try {
            context.stop();
        } catch (LifecycleException e) {
            throw MESSAGES.stopContextPhaseFailed(e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            throw MESSAGES.destroyContextPhaseFailed(e);
        }
    }

    private static class LocalInstanceManager implements InstanceManager {
        LocalInstanceManager() {
        }

        @Override
        public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException, NamingException,
                InstantiationException, ClassNotFoundException {
            return Class.forName(className).newInstance();
        }

        @Override
        public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException,
                InvocationTargetException, NamingException, InstantiationException, ClassNotFoundException {
            return Class.forName(fqcn, false, classLoader).newInstance();
        }

        @Override
        public Object newInstance(Class<?> c) throws IllegalAccessException, InvocationTargetException, NamingException,
                InstantiationException {
            return c.newInstance();
        }

        @Override
        public void newInstance(Object o) throws IllegalAccessException, InvocationTargetException, NamingException {
            throw new IllegalStateException();
        }

        @Override
        public void destroyInstance(Object o) throws IllegalAccessException, InvocationTargetException {
        }
    }
}
