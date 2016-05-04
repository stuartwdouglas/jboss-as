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

package org.jboss.as.ejb3.remote.protocol.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.component.EJBComponentUnavailableException;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.protocol.MarshallingSupport;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.invocation.InterceptorContext;
import org.jboss.marshalling.AbstractClassResolver;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.httpinvocation.RequestMessage;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.xnio.IoUtils;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Stuart Douglas
 */
public class HttpMethodInvocationMessageHandler extends AbstractEjbHttpMessageHandler {

    private final ExecutorService executorService;
    private final MarshallerFactory marshallerFactory;
    private final DeploymentRepository deploymentRepository;

    public static final HttpString ALLOW_CANCELLATION = new HttpString("X-wf-ejb-allow-cancellation");
    public static final String EJB_RESPONSE_V1 = "application/x-wf-ejb-response; version=1";
    public static final String EJB_REQUEST = "application/x-wf-ejb-request";

    public HttpMethodInvocationMessageHandler(final DeploymentRepository deploymentRepository, final MarshallerFactory marshallerFactory, final ExecutorService executorService) {
        this.deploymentRepository  = deploymentRepository;
        this.executorService = executorService;
        this.marshallerFactory = marshallerFactory;
    }

    @Override
    public void handle(HttpServerExchange exchange, RequestMessage message) throws IOException {
        //TODO: verify Accept header
        String url = exchange.getRelativePath();
        String[] parts = url.split("/");
        if(parts.length < 7) {
            throw EjbLogger.ROOT_LOGGER.invalidUrl(url);
        }
        String appName = parts[0].equals("-") ? "" : parts[0];
        String moduleName = parts[1].equals("-") ? "" : parts[1];
        String distinctName = parts[2].equals("-") ? "" : parts[2];
        String beanName = parts[3];
        String sfsbSessionId = parts[4];
        String viewClassName = parts[5];
        String methodName = parts[6];
        String[] methodParamTypes = new String[parts.length - 7];
        for(int i = 7; i < methodParamTypes.length; ++i) {
            methodParamTypes[i - 7] = parts[i];
        }
        String asyncHeader = exchange.getRequestHeaders().getFirst(ALLOW_CANCELLATION);
        final boolean asyncMethodSupported = asyncHeader != null && "true".equals(asyncHeader);

        // read the Locator
        // we use a mutable ClassResolver, so that we can switch to a different (and correct deployment CL)
        // midway through the unmarshalling of the stream
        final ClassLoaderSwitchingClassResolver classResolver = new ClassLoaderSwitchingClassResolver(Thread.currentThread().getContextClassLoader());
        final Unmarshaller unmarshaller = MarshallingSupport.prepareForUnMarshalling(this.marshallerFactory, classResolver, new DataInputStream(exchange.getInputStream()));

        final EjbDeploymentInformation ejbDeploymentInformation = findEJB(deploymentRepository, appName, moduleName, distinctName, beanName);
        if (ejbDeploymentInformation == null) {
            sendError(marshallerFactory, exchange, null, StatusCodes.NOT_FOUND);
            return;
        }
        final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        Runnable runnable = null;
        try {
            //set the correct TCCL for unmarshalling
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(ejbDeploymentInformation.getDeploymentClassLoader());
            // now switch the CL to the EJB deployment's CL so that the unmarshaller can use the
            // correct CL for the rest of the unmarshalling of the stream
            classResolver.switchClassLoader(ejbDeploymentInformation.getDeploymentClassLoader());

            // Make sure it's a remote view
            if (!ejbDeploymentInformation.isRemoteView(viewClassName)) {
                //TODO: send a more informative error message
                sendError(marshallerFactory, exchange, null, StatusCodes.NOT_FOUND);
                return;
            }
            final ComponentView componentView = ejbDeploymentInformation.getView(viewClassName);

            // read the Locator
            Class<?> view;
            try {
                view = ejbDeploymentInformation.getDeploymentClassLoader().loadClass(viewClassName);
            } catch (ClassNotFoundException e) {
                sendError(marshallerFactory, exchange, e, StatusCodes.NOT_FOUND);
                return;
            }
            EJBLocator locator;
            if(sfsbSessionId.equals("-")) {
                if(EJBHome.class.isAssignableFrom(view)) {
                    locator = new EJBHomeLocator(view, appName, moduleName, beanName, distinctName);
                } else {
                    locator = new StatelessEJBLocator(view, appName, moduleName, beanName, distinctName);
                }
            } else {
                locator = new StatefulEJBLocator(view, appName, moduleName, beanName, distinctName, SessionID.createSessionID(Base64.getDecoder().decode(sfsbSessionId)), null, null);
            }


            final Method invokedMethod = this.findMethod(componentView, methodName, methodParamTypes);
            if (invokedMethod == null) {
                sendError(marshallerFactory, exchange, null, StatusCodes.NOT_FOUND);
                return;
            }

            final Object[] methodParams = new Object[methodParamTypes.length];
            // un-marshall the method arguments
            if (methodParamTypes.length > 0) {
                for (int i = 0; i < methodParamTypes.length; i++) {
                    try {
                        methodParams[i] = unmarshaller.readObject();
                    } catch (Throwable e) {
                        // write out the failure
                        sendError(marshallerFactory, exchange, e, StatusCodes.BAD_REQUEST);
                        return;
                    }
                }
            }
            // read the attachments
            final Map<String, Object> attachments;
            try {
                attachments = MarshallingSupport.readAttachments(unmarshaller);
            } catch (Throwable e) {
                // write out the failure
                sendError(marshallerFactory, exchange, e, StatusCodes.BAD_REQUEST);
                return;
            }
            // done with unmarshalling
            unmarshaller.finish();

            runnable = new Runnable() {

                @Override
                public void run() {
                    // check if it's async. If yes, then notify the client that's it's async method (so that
                    // it can unblock if necessary)
                    boolean cancelSupported = componentView.isAsynchronous(invokedMethod) && asyncMethodSupported;
                    // invoke the method
                    Object result = null;
                    //TODO: security support
                    try {
                        result = invokeMethod(componentView, invokedMethod, methodParams, locator, attachments, asyncMethodSupported);
                    } catch (Throwable throwable) {
                        try {
                            // if the EJB is shutting down when the invocation was done, then it's as good as the EJB not being available. The client has to know about this as
                            // a "no such EJB" failure so that it can retry the invocation on a different node if possible.
                            if (throwable instanceof EJBComponentUnavailableException) {
                                EjbLogger.EJB3_INVOCATION_LOGGER.debugf("Cannot handle method invocation: %s on bean: %s due to EJB component unavailability exception. Returning a no such EJB available message back to client", invokedMethod, beanName);
                                sendError(marshallerFactory, exchange, null, StatusCodes.SERVICE_UNAVAILABLE);
                            } else {
                                // write out the failure
                                Throwable throwableToWrite = throwable;
                                final Throwable cause = throwable.getCause();
                                if (componentView.getComponent() instanceof StatefulSessionComponent && throwable instanceof EJBException && cause != null) {
                                    if (!(componentView.getComponent().isRemotable(cause))) {
                                        // Avoid serializing the cause of the exception in case it is not remotable
                                        // Client might not be able to deserialize and throw ClassNotFoundException
                                        throwableToWrite = new EJBException(throwable.getLocalizedMessage());
                                    }
                                }
                                sendError(marshallerFactory, exchange, throwable, StatusCodes.INTERNAL_SERVER_ERROR); //TODO: exception attachments?
                            }
                        } catch (Throwable ioe) {
                            // we couldn't write out a method invocation failure message. So let's at least log the
                            // actual method invocation exception, for debugging/reference
                            EjbLogger.REMOTE_LOGGER.errorInvokingMethod(throwable, invokedMethod, beanName, appName, moduleName, distinctName);
                            // now log why we couldn't send back the method invocation failure message
                            EjbLogger.REMOTE_LOGGER.couldNotWriteMethodInvocation(ioe, invokedMethod, beanName, appName, moduleName, distinctName);
                        }
                        return;
                    }
                    // write out the (successful) method invocation result to the channel output stream
                    try {
                        // attach any weak affinity if available
                        Affinity weakAffinity = null;
                        if (locator instanceof StatefulEJBLocator && componentView.getComponent() instanceof StatefulSessionComponent) {
                            final StatefulSessionComponent statefulSessionComponent = (StatefulSessionComponent) componentView.getComponent();
                            weakAffinity = HttpMethodInvocationMessageHandler.this.getWeakAffinity(statefulSessionComponent, (StatefulEJBLocator<?>) locator);
                        } else if (componentView.getComponent() instanceof StatelessSessionComponent) {
                            final StatelessSessionComponent statelessSessionComponent = (StatelessSessionComponent) componentView.getComponent();
                            weakAffinity = statelessSessionComponent.getWeakAffinity();
                        }
                        if (weakAffinity != null) {
                            attachments.put(Affinity.WEAK_AFFINITY_CONTEXT_KEY, weakAffinity);
                        }
                        writeMethodInvocationResponse(result, attachments, exchange);
                    } catch (Throwable ioe) {
                        boolean isAsyncVoid = componentView.isAsynchronous(invokedMethod) && invokedMethod.getReturnType().equals(Void.TYPE);
                        if (!isAsyncVoid)
                            EjbLogger.REMOTE_LOGGER.couldNotWriteMethodInvocation(ioe, invokedMethod, beanName, appName, moduleName, distinctName);
                        return;
                    }
                }
            };
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
        }
        // invoke the method and write out the response on a separate thread
        if(executorService != null) {
            executorService.submit( runnable );
        } else {
            runnable.run();
        }
    }

    private Affinity getWeakAffinity(final StatefulSessionComponent statefulSessionComponent, final StatefulEJBLocator<?> statefulEJBLocator) {
        final SessionID sessionID = statefulEJBLocator.getSessionId();
        return statefulSessionComponent.getCache().getWeakAffinity(sessionID);
    }

    private Object invokeMethod(final ComponentView componentView, final Method method, final Object[] args, final EJBLocator<?> ejbLocator, final Map<String, Object> attachments, boolean supportsCancellation) throws Throwable {
        final InterceptorContext interceptorContext = new InterceptorContext();
        interceptorContext.setParameters(args);
        interceptorContext.setMethod(method);
        interceptorContext.putPrivateData(Component.class, componentView.getComponent());
        interceptorContext.putPrivateData(ComponentView.class, componentView);
        interceptorContext.putPrivateData(InvocationType.class, InvocationType.REMOTE);
        // setup the contextData on the (spec specified) InvocationContext
        final Map<String, Object> invocationContextData = new HashMap<String, Object>();
        interceptorContext.setContextData(invocationContextData);
        if (attachments != null) {
            // attach the attachments which were passed from the remote client
            for (final Map.Entry<String, Object> attachment : attachments.entrySet()) {
                if (attachment == null) {
                    continue;
                }
                final String key = attachment.getKey();
                final Object value = attachment.getValue();
                // these are private to JBoss EJB implementation and not meant to be visible to the
                // application, so add these attachments to the privateData of the InterceptorContext
                if (EJBClientInvocationContext.PRIVATE_ATTACHMENTS_KEY.equals(key)) {
                    final Map<?, ?> privateAttachments = (Map<?, ?>) value;
                    for (final Map.Entry<?, ?> privateAttachment : privateAttachments.entrySet()) {
                        interceptorContext.putPrivateData(privateAttachment.getKey(), privateAttachment.getValue());
                    }
                } else {
                    // add it to the InvocationContext which will be visible to the target bean and the
                    // application specific interceptors
                    invocationContextData.put(key, value);
                }
            }
        }
        // add the session id to the interceptor context, if it's a stateful ejb locator
        if (ejbLocator instanceof StatefulEJBLocator) {
            interceptorContext.putPrivateData(SessionID.class, ((StatefulEJBLocator<?>) ejbLocator).getSessionId());
        }
        //TODO: support for cancellation
        return componentView.invoke(interceptorContext);
    }

    private Method findMethod(final ComponentView componentView, final String methodName, final String[] paramTypes) {
        final Set<Method> viewMethods = componentView.getViewMethods();
        for (final Method method : viewMethods) {
            if (method.getName().equals(methodName)) {
                final Class<?>[] methodParamTypes = method.getParameterTypes();
                if (methodParamTypes.length != paramTypes.length) {
                    continue;
                }
                boolean found = true;
                for (int i = 0; i < methodParamTypes.length; i++) {
                    if (!methodParamTypes[i].getName().equals(paramTypes[i])) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return method;
                }
            }
        }
        return null;
    }

    private void writeMethodInvocationResponse(final Object result, final Map<String, Object> attachments, HttpServerExchange exchange) throws IOException {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, EJB_RESPONSE_V1);

        final DataOutputStream outputStream = new DataOutputStream(exchange.getOutputStream());
        try {
            final Marshaller marshaller = MarshallingSupport.prepareForMarshalling(marshallerFactory, outputStream);
            marshaller.writeObject(result);
            // write the attachments
            MarshallingSupport.writeAttachments(marshaller, attachments);
            // finish marshalling
            marshaller.finish();
        } finally {
            IoUtils.safeClose(outputStream);
        }
    }

    /**
     * A mutable {@link org.jboss.marshalling.ClassResolver}
     */
    private class ClassLoaderSwitchingClassResolver extends AbstractClassResolver {

        private ClassLoader currentClassLoader;

        ClassLoaderSwitchingClassResolver(final ClassLoader classLoader) {
            this.currentClassLoader = classLoader;
        }

        /**
         * Sets the passed <code>newCL</code> as the classloader which will be returned on
         * subsequent calls to {@link #getClassLoader()}
         *
         * @param newCL
         */
        void switchClassLoader(final ClassLoader newCL) {
            this.currentClassLoader = newCL;
        }

        @Override
        protected ClassLoader getClassLoader() {
            return this.currentClassLoader;
        }
    }
}
