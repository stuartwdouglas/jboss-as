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

package org.jboss.as.test.integration.ejb.remote.client.api;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.StatefulEJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Tests the various common use cases of the EJB remote client API
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBClientAPIStressTestCase {

    private static final Logger logger = Logger.getLogger(EJBClientAPIStressTestCase.class);

    private static final String APP_NAME = "ejb-remote-client-api-stress-test";

    private static final String MODULE_NAME = "ejb";

    /**
     * Creates an EJB deployment
     *
     * @return
     */
    @Deployment
    public static Archive<?> createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(EJBClientAPIStressTestCase.class.getPackage());
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        ear.addAsModule(jar);

        return ear;
    }

    @Test
    public void connectionStressTest() throws Exception {
        for (int i = 0; i < 1000; ++i) {
            try {
            Endpoint endpoint = Remoting.createEndpoint("endpoint", OptionMap.EMPTY);
            endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));

            // open a connection
            final IoFuture<Connection> futureConnection = endpoint.connect(new URI("remote://localhost:4447"), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE, Options.SASL_POLICY_NOPLAINTEXT, Boolean.FALSE), new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for(final Callback callback : callbacks) {
                        if(callback instanceof NameCallback) {
                            ((NameCallback) callback).setName("anonymous");
                        }
                    }
                }
            });
            Connection connection = IoFutureHelper.get(futureConnection, 30L, TimeUnit.SECONDS);

            final EJBClientContext ejbClientContext = EJBClientContext.create();
            ejbClientContext.registerConnection(connection);

            EJBClientContext ctx = EJBClientContext.create();
            ctx.registerConnection(connection);
            EJBClientContext.setConstantContext(ctx);

            final StatelessEJBLocator<EchoRemote> locator = new StatelessEJBLocator(EchoRemote.class, APP_NAME, MODULE_NAME, EchoBean.class.getSimpleName(), "");
            final EchoRemote proxy = EJBClient.createProxy(locator);
            Assert.assertNotNull("Received a null proxy", proxy);
            final String message = "Hello world from a really remote client";
            final String echo = proxy.echo(message);
            Assert.assertEquals("Unexpected echo message", message, echo);


            connection.close();
            endpoint.close();
            } catch (Exception e) {
                throw new Exception("Failed on run " + i, e);
            }
        }
    }

}
