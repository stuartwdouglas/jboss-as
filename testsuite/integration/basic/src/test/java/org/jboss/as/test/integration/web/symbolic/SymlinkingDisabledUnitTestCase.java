package org.jboss.as.test.integration.web.symbolic;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author navssurtani
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SymlinkingDisabledUnitTestCase extends AbstractSymbolicLinkingUnitTestCase{

    private static Logger log = Logger.getLogger(SymlinkingDisabledUnitTestCase.class);

    @Deployment(name = "symbolic-disabled.war", testable = false)
    public static Archive<?> getDeployment() {
        // Now we should be able to add the symbolic linked deployment file to ShrinkWrap.
        return ShrinkWrap.create(WebArchive.class, "symbolic-disabled.war")
                       .addAsResource(symbolic)
                       .setWebXML("jboss-web.xml");
    }

    @Test
    @OperateOnDeployment("symbolic-disabled.war")
    public void testSymlinkingDisabled() {

    }


}
