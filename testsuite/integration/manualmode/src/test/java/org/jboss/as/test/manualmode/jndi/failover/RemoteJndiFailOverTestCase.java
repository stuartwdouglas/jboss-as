/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.jndi.failover;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that remote JNDI failover works correctly
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteJndiFailOverTestCase {

    private static final Logger logger = Logger.getLogger(RemoteJndiFailOverTestCase.class);


    private static final String DEFAULT_JBOSSAS = "default-jbossas";
    private static final String JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION = "jbossas-with-remote-outbound-connection";
    private static final String MODULE_NAME = "failOverTest";
    private static final String MESSAGE_TOKEN = "#MESSAGE#";
    private static final String MESSAGE1 = "server1";
    private static final String MESSAGE2 = "server2";
    private static final int TASKS = 50;


    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private static ExecutorService executor;

    @BeforeClass
    public static void before() {
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterClass
    public static void after() {
        executor.shutdown();
    }


    @Deployment(name = DEFAULT_JBOSSAS, managed = false, testable = false)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive createContainer1Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(RemoteJndiFailOverTestCase.class.getPackage());
        ejbJar.addAsManifestResource(new StringAsset(FileUtils.readFile(RemoteJndiFailOverTestCase.class, "ejb-jar.xml").replace(MESSAGE_TOKEN, MESSAGE1)), "ejb-jar.xml");
        return ejbJar;
    }

    @Deployment(name = JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION, managed = false, testable = false)
    @TargetsContainer(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION)
    public static Archive createContainer2Deployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(RemoteJndiFailOverTestCase.class.getPackage());
        ejbJar.addAsManifestResource(new StringAsset(FileUtils.readFile(RemoteJndiFailOverTestCase.class, "ejb-jar.xml").replace(MESSAGE_TOKEN, MESSAGE2)), "ejb-jar.xml");
        return ejbJar;
    }


    @Test
    public void testRemoteJNDIFailOver() throws Exception {


        final GetMessageTask task = new GetMessageTask();

        // First start the default server
        this.container.start(DEFAULT_JBOSSAS);
        try {
            this.deployer.deploy(DEFAULT_JBOSSAS);

            //make sure the invocation can run against the started server
            Assert.assertEquals(MESSAGE1, task.call());
            runMultiThreaded(task, MESSAGE1);

            // start the other server
            this.container.start(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
            this.deployer.deploy(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);

            Assert.assertEquals(MESSAGE1, task.call());

            //stop the first container
            this.container.stop(DEFAULT_JBOSSAS);

            Assert.assertEquals(MESSAGE2, task.call());
            runMultiThreaded(task, MESSAGE2);

            this.deployer.undeploy(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
            this.container.stop(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);

            //now we have no containers
            try {
                task.call();
                Assert.fail("No servers running, lookup should fail");
            } catch (NamingException expected) {

            }

            this.container.start(DEFAULT_JBOSSAS);
            Assert.assertEquals(MESSAGE1, task.call());
            this.deployer.undeploy(DEFAULT_JBOSSAS);
            this.container.stop(DEFAULT_JBOSSAS);

        } finally {
            try {
                this.deployer.undeploy(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
                this.container.stop(JBOSSAS_WITH_REMOTE_OUTBOUND_CONNECTION);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }
            try {
                this.deployer.undeploy(DEFAULT_JBOSSAS);
                this.container.stop(DEFAULT_JBOSSAS);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }
        }

    }

    private void runMultiThreaded(final GetMessageTask task, String message) throws InterruptedException, ExecutionException {
        final List<Future<String>> results = new ArrayList<Future<String>>();
        for (int i = 0; i < TASKS; ++i) {
            results.add(executor.submit(task));
        }
        for (final Future<String> result : results) {
            Assert.assertEquals(message, result.get());
        }
    }

    private static class GetMessageTask implements Callable<String> {

        @Override
        public String call() throws Exception {
            final Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
            env.put(Context.PROVIDER_URL, "remote://" + System.getProperty("node0") + ":4447,remote://" + System.getProperty("node1") + ":4547");
            env.put("jboss.naming.client.ejb.context", true);
            env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
            env.put("jboss.naming.client.security.callback.handler.class", CallbackHandler.class.getName());
            final InitialContext initialContext = new InitialContext(env);
            try {
                SimpleRemoteInterface remote = (SimpleRemoteInterface) initialContext.lookup(MODULE_NAME + "/" + SimpleRemoteEjb.class.getSimpleName() + "!" + SimpleRemoteInterface.class.getName());
                return remote.getMessage();
            } finally {
                initialContext.close();
            }
        }
    }

}
