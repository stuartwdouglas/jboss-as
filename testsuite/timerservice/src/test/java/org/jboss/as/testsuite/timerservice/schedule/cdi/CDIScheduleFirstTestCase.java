/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.testsuite.timerservice.schedule.cdi;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests that CDI interceptors work on timeout methods
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class CDIScheduleFirstTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return createDeployment(CDIScheduleFirstTestCase.class);
    }

    public static Archive<?> createDeployment(final Class<?> testClass) {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class,"testCdiInterceptor.jar");
        jar.addPackage(CdiBean.class.getPackage());
        jar.addAsManifestResource(new StringAsset("<beans><interceptors><class>" + CdiInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml");
        return jar;

    }

    @Test
    public void testScheduleAnnotation() throws NamingException {
        InitialContext ctx = new InitialContext();
        CDIScheduleBean bean = (CDIScheduleBean)ctx.lookup("java:module/" + CDIScheduleBean.class.getSimpleName());
        Assert.assertTrue(CDIScheduleBean.awaitTimerCall());
        Assert.assertTrue(CdiInterceptor.interceptorCalled);
    }

}
