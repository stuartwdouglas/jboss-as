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

package org.jboss.as.test.integration.ejb.stateful.passivation.ejb2;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.stateful.passivation.PassivationSucceedsUnitTestCaseSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that passivation succeeds for ejb2 beans.
 * @see org.jboss.as.test.integration.ejb.stateful.passivation.PassivationSucceedsUnitTestCase
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@ServerSetup(PassivationSucceedsUnitTestCaseSetup.class)
public class PassivationSucceedsEJB2TestCase {
    private static final Logger log = Logger.getLogger(PassivationSucceedsEJB2TestCase.class);
    private static String jndi;
    
    @ArquillianResource
    private InitialContext ctx;
    
    static {
        jndi = "java:module/" + TestPassivationBean.class.getSimpleName() + "!" + TestPassivationRemoteHome.class.getName();
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "passivation-ejb2-test.jar");
        jar.addPackage(PassivationSucceedsEJB2TestCase.class.getPackage());
        jar.addClasses(PassivationSucceedsUnitTestCaseSetup.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"),
                "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testPassivationMaxSize() throws Exception {
        TestPassivationRemoteHome home = (TestPassivationRemoteHome) ctx.lookup(jndi);
        TestPassivationRemote remote1 = home.create();
        Assert.assertEquals("Returned remote1 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote1.returnTrueString());

        TestPassivationRemoteHome home2 = (TestPassivationRemoteHome) ctx.lookup(jndi);
        TestPassivationRemote remote2 = home2.create();
        Assert.assertEquals("Returned remote2 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote2.returnTrueString());

        // create another bean. This should force the other bean to passivate, as only one bean is allowed in the pool at a time
        ctx.lookup(jndi);

        Assert.assertTrue("ejbPassivate not called on remote1, check cache configuration and client sleep time",
                remote1.hasBeenPassivated());
        Assert.assertTrue("ejbPassivate not called on remote2, check cache configuration and client sleep time",
                remote2.hasBeenPassivated());
        // Assert.assertTrue("ejbActivate not called on remote1", remote1.hasBeenActivated());
        // Assert.assertTrue("ejbActivate not called on remote2", remote2.hasBeenActivated());

        remote1.remove();
        remote2.remove();
    }

    @Test
    public void testPassivationIdleTimeout() throws Exception {
        // Lookup and create stateful instance of ejb2 bean
        TestPassivationRemoteHome home = (TestPassivationRemoteHome) ctx.lookup(jndi);
        TestPassivationRemote remote = home.create();
        
        // Make an invocation
        Assert.assertEquals("Returned result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote.returnTrueString());
        // Sleep, allow SFSB to passivate
        Thread.sleep(1600L);
        // Make another invocation
        Assert.assertEquals("Returned result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote.returnTrueString());
        
        Assert.assertTrue("ejbActivate not called, check CacheConfig and client sleep time", remote.hasBeenActivated());
        Assert.assertTrue("ejbPassivate not called, check CacheConfig and client sleep time", remote.hasBeenPassivated());
        remote.remove();
    }
}
