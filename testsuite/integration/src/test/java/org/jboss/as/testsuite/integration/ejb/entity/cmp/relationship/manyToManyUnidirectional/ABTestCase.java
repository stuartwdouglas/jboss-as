/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.manyToManyUnidirectional;

import java.util.Iterator;
import java.util.Properties;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ABTestCase {
    static org.jboss.logging.Logger log = org.jboss.logging.Logger.getLogger(ABTestCase.class);

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-relationship.jar");
        jar.addPackage(org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.manyToManyBidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.manyToManyUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.manyToOneUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.oneToManyBidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.oneToManyUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.oneToOneUnidirectional.ABTestCase.class.getPackage());
        jar.addPackage(org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.oneToOneBidirectional.ABTestCase.class.getPackage());
        jar.addAsManifestResource("ejb/entity/cmp/relationship/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/entity/cmp/relationship/jboss.xml", "jboss.xml");
        jar.addAsManifestResource("ejb/entity/cmp/relationship/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    private AHome getAHome() {
        try {
            return (AHome) iniCtx.lookup("java:module/A_ManyToMany_Uni_EJB!org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.manyToManyUnidirectional.AHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getAHome: " + e.getMessage());
        }
        return null;
    }

    private BHome getBHome() {
        try {
            return (BHome) iniCtx.lookup("java:module/B_ManyToMany_Uni_EJB!org.jboss.as.testsuite.integration.ejb.entity.cmp.relationship.manyToManyUnidirectional.BHome");
        } catch (Exception e) {
            log.debug("failed", e);
            fail("Exception in getBHome: " + e.getMessage());
        }
        return null;
    }

    // a1.setB(a3.getB());
    public void test_a1SetB_a3GetB() throws Exception {
        AHome aHome = getAHome();
        BHome bHome = getBHome();

        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));
        A a3 = aHome.create(new Integer(3));
        A a4 = aHome.create(new Integer(4));
        A a5 = aHome.create(new Integer(5));

        B b1 = bHome.create(new Integer(-1));
        B b2 = bHome.create(new Integer(-2));
        B b3 = bHome.create(new Integer(-3));
        B b4 = bHome.create(new Integer(-4));
        B b5 = bHome.create(new Integer(-5));

        a1.getB().add(b1);
        a1.getB().add(b2);
        a2.getB().add(b1);
        a2.getB().add(b2);
        a2.getB().add(b3);
        a3.getB().add(b2);
        a3.getB().add(b3);
        a3.getB().add(b4);
        a4.getB().add(b3);
        a4.getB().add(b4);
        a4.getB().add(b5);
        a5.getB().add(b4);
        a5.getB().add(b5);

        assertTrue(a1.getB().contains(b1));
        assertTrue(a1.getB().contains(b2));
        assertTrue(a2.getB().contains(b1));
        assertTrue(a2.getB().contains(b2));
        assertTrue(a2.getB().contains(b3));
        assertTrue(a3.getB().contains(b2));
        assertTrue(a3.getB().contains(b3));
        assertTrue(a3.getB().contains(b4));
        assertTrue(a4.getB().contains(b3));
        assertTrue(a4.getB().contains(b4));
        assertTrue(a4.getB().contains(b5));
        assertTrue(a5.getB().contains(b4));
        assertTrue(a5.getB().contains(b5));

        // Change:
        a1.setB(a3.getB());

        // Expected result:
        assertTrue(!a1.getB().contains(b1));
        assertTrue(a1.getB().contains(b2));
        assertTrue(a1.getB().contains(b3));
        assertTrue(a1.getB().contains(b4));

        assertTrue(a2.getB().contains(b1));
        assertTrue(a2.getB().contains(b2));
        assertTrue(a2.getB().contains(b3));

        assertTrue(a3.getB().contains(b2));
        assertTrue(a3.getB().contains(b3));
        assertTrue(a3.getB().contains(b4));

        assertTrue(a4.getB().contains(b3));
        assertTrue(a4.getB().contains(b4));
        assertTrue(a4.getB().contains(b5));

        assertTrue(a5.getB().contains(b4));
        assertTrue(a5.getB().contains(b5));
    }

    // a1.getB().add(b3);
    public void test_a1GetB_addB3() throws Exception {
        AHome aHome = getAHome();
        BHome bHome = getBHome();

        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));
        A a3 = aHome.create(new Integer(3));
        A a4 = aHome.create(new Integer(4));
        A a5 = aHome.create(new Integer(5));

        B b1 = bHome.create(new Integer(-1));
        B b2 = bHome.create(new Integer(-2));
        B b3 = bHome.create(new Integer(-3));
        B b4 = bHome.create(new Integer(-4));
        B b5 = bHome.create(new Integer(-5));

        a1.getB().add(b1);
        a1.getB().add(b2);
        a2.getB().add(b1);
        a2.getB().add(b2);
        a2.getB().add(b3);
        a3.getB().add(b2);
        a3.getB().add(b3);
        a3.getB().add(b4);
        a4.getB().add(b3);
        a4.getB().add(b4);
        a4.getB().add(b5);
        a5.getB().add(b4);
        a5.getB().add(b5);

        assertTrue(a1.getB().contains(b1));
        assertTrue(a1.getB().contains(b2));
        assertTrue(a2.getB().contains(b1));
        assertTrue(a2.getB().contains(b2));
        assertTrue(a2.getB().contains(b3));
        assertTrue(a3.getB().contains(b2));
        assertTrue(a3.getB().contains(b3));
        assertTrue(a3.getB().contains(b4));
        assertTrue(a4.getB().contains(b3));
        assertTrue(a4.getB().contains(b4));
        assertTrue(a4.getB().contains(b5));
        assertTrue(a5.getB().contains(b4));
        assertTrue(a5.getB().contains(b5));

        // Change:
        a1.getB().add(b3);

        // Expected result:
        assertTrue(a1.getB().contains(b1));
        assertTrue(a1.getB().contains(b2));
        assertTrue(a1.getB().contains(b3));

        assertTrue(a2.getB().contains(b1));
        assertTrue(a2.getB().contains(b2));
        assertTrue(a2.getB().contains(b3));

        assertTrue(a3.getB().contains(b2));
        assertTrue(a3.getB().contains(b3));
        assertTrue(a3.getB().contains(b4));

        assertTrue(a4.getB().contains(b3));
        assertTrue(a4.getB().contains(b4));
        assertTrue(a4.getB().contains(b5));

        assertTrue(a5.getB().contains(b4));
        assertTrue(a5.getB().contains(b5));
    }

    // a2.getB().remove(b2);
    public void test_a2GetB_removeB2() throws Exception {
        AHome aHome = getAHome();
        BHome bHome = getBHome();

        // Before change:
        A a1 = aHome.create(new Integer(1));
        A a2 = aHome.create(new Integer(2));
        A a3 = aHome.create(new Integer(3));
        A a4 = aHome.create(new Integer(4));
        A a5 = aHome.create(new Integer(5));

        B b1 = bHome.create(new Integer(-1));
        B b2 = bHome.create(new Integer(-2));
        B b3 = bHome.create(new Integer(-3));
        B b4 = bHome.create(new Integer(-4));
        B b5 = bHome.create(new Integer(-5));

        a1.getB().add(b1);
        a1.getB().add(b2);
        a2.getB().add(b1);
        a2.getB().add(b2);
        a2.getB().add(b3);
        a3.getB().add(b2);
        a3.getB().add(b3);
        a3.getB().add(b4);
        a4.getB().add(b3);
        a4.getB().add(b4);
        a4.getB().add(b5);
        a5.getB().add(b4);
        a5.getB().add(b5);

        assertTrue(a1.getB().contains(b1));
        assertTrue(a1.getB().contains(b2));
        assertTrue(a2.getB().contains(b1));
        assertTrue(a2.getB().contains(b2));
        assertTrue(a2.getB().contains(b3));
        assertTrue(a3.getB().contains(b2));
        assertTrue(a3.getB().contains(b3));
        assertTrue(a3.getB().contains(b4));
        assertTrue(a4.getB().contains(b3));
        assertTrue(a4.getB().contains(b4));
        assertTrue(a4.getB().contains(b5));
        assertTrue(a5.getB().contains(b4));
        assertTrue(a5.getB().contains(b5));

        // Change:
        a2.getB().remove(b2);

        // Expected result:
        assertTrue(a1.getB().contains(b1));
        assertTrue(a1.getB().contains(b2));

        assertTrue(a2.getB().contains(b1));
        assertTrue(!a2.getB().contains(b2));
        assertTrue(a2.getB().contains(b3));

        assertTrue(a3.getB().contains(b2));
        assertTrue(a3.getB().contains(b3));
        assertTrue(a3.getB().contains(b4));

        assertTrue(a4.getB().contains(b3));
        assertTrue(a4.getB().contains(b4));
        assertTrue(a4.getB().contains(b5));

        assertTrue(a5.getB().contains(b4));
        assertTrue(a5.getB().contains(b5));
    }

    public void setUpEJB(Properties props) throws Exception {
        deleteAllAsAndBs(getAHome(), getBHome());
    }

    public void deleteAllAsAndBs(AHome aHome, BHome bHome) throws Exception {
        // delete all As
        Iterator currentAs = aHome.findAll().iterator();
        while (currentAs.hasNext()) {
            A a = (A) currentAs.next();
            a.remove();
        }

        // delete all Bs
        Iterator currentBs = bHome.findAll().iterator();
        while (currentBs.hasNext()) {
            B b = (B) currentBs.next();
            b.remove();
        }
    }
}



