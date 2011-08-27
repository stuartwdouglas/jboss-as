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

package org.jboss.as.testsuite.integration.ejb.entity.bmp;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import java.util.Collection;

/**
 * Tests bean managed persistence
 */
@RunWith(Arquillian.class)
public class BMPEntityBeanTestCase {

    private static final String ARCHIVE_NAME = "SimpleLocalHomeTest.war";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(BMPEntityBeanTestCase.class.getPackage());
        war.addAsWebInfResource("ejb/entity/bmp/ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testSimpleCreate() throws Exception {
        final BMPLocalHome home = (BMPLocalHome) iniCtx.lookup("java:module/SimpleBMP!" + BMPLocalHome.class.getName());
        final BMPLocalInterface ejbInstance = home.createWithValue("Hello");
        final Integer pk = (Integer) ejbInstance.getPrimaryKey();
        Assert.assertEquals("Hello", DataStore.DATA.get(pk));
    }

    @Test
    public void testFindByPrimaryKey() throws Exception {
        final BMPLocalHome home = (BMPLocalHome) iniCtx.lookup("java:module/SimpleBMP!" + BMPLocalHome.class.getName());
        DataStore.DATA.put(1099, "VALUE1099");
        BMPLocalInterface result = home.findByPrimaryKey(1099);
        Assert.assertEquals("VALUE1099", result.getMyField());
    }

    @Test
    public void testSingleResultFinderMethod() throws Exception {
        final BMPLocalHome home = (BMPLocalHome) iniCtx.lookup("java:module/SimpleBMP!" + BMPLocalHome.class.getName());
        DataStore.DATA.put(888, "VALUE888");
        BMPLocalInterface result = home.findByValue("VALUE888");
        Assert.assertEquals("VALUE888", result.getMyField());
        Assert.assertEquals(888, result.getPrimaryKey());
    }

    @Test
    public void testCollectionFinderMethod() throws Exception {
        final BMPLocalHome home = (BMPLocalHome) iniCtx.lookup("java:module/SimpleBMP!" + BMPLocalHome.class.getName());
        DataStore.DATA.put(1000, "Collection");
        DataStore.DATA.put(1001, "Collection");
        Collection<BMPLocalInterface> col = home.findCollection();
        for (BMPLocalInterface result : col) {
            Assert.assertEquals("Collection", result.getMyField());
        }
    }

    @Test
    public void testRemoveEntityBean() throws Exception {
        final BMPLocalHome home = (BMPLocalHome) iniCtx.lookup("java:module/SimpleBMP!" + BMPLocalHome.class.getName());
        DataStore.DATA.put(56, "Remove");
        BMPLocalInterface result = home.findByPrimaryKey(56);
        Assert.assertEquals("Remove", result.getMyField());
        result.remove();
        Assert.assertFalse(DataStore.DATA.containsKey(56));
        try {
            result.getMyField();
            throw new RuntimeException("Expected invocation on removed instance to fail");
        } catch (NoSuchEJBException expected) {

        }
    }
}
