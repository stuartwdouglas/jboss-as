package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.util.concurrent.TimeUnit;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class GetTimersTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "gettimers.jar");
        jar.addPackage(GetTimersTestCase.class.getPackage());


        return jar;
    }

    @Test
    public void testGetTimers() throws Exception {

        Timer1 t1 = (Timer1) new InitialContext().lookup("java:module/Timer1");
        t1.start("t1");
        Timer2 t2 = (Timer2) new InitialContext().lookup("java:module/Timer2");
        t2.start("t2");
        Timer3 t3 = (Timer3) new InitialContext().lookup("java:module/Timer3");
        t3.start("t3");
        Timer4 t4 = (Timer4) new InitialContext().lookup("java:module/Timer4");
        t4.start("t4");
        Assert.assertEquals("t1", Timer1.DATA.poll(10, TimeUnit.SECONDS));
        Assert.assertEquals("t2", Timer2.DATA.poll(10, TimeUnit.SECONDS));
        Assert.assertEquals("t3", Timer3.DATA.poll(10, TimeUnit.SECONDS));
        Assert.assertEquals("t4", Timer4.DATA.poll(10, TimeUnit.SECONDS));

    }


}
