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

import java.io.IOException;

/**
 * @author navssurtani
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SymlinkingEnabledUnitTestCase extends AbstractSymbolicLinkingUnitTestCase{

    private static Logger log = Logger.getLogger(SymlinkingEnabledUnitTestCase.class);

    @Deployment(name = "symbolic-enabled.war", testable = false)
    public static Archive<?> getDeployment() {
        try {
            symbolic = getSymbolicLink();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }

        assert symbolic.exists() : "Symlinked file doesn't exist.";

        // Now we should be able to add the symbolic linked deployment file to ShrinkWrap.
        WebArchive war = ShrinkWrap.create(WebArchive.class, "symbolic-enabled.war");
        war.addAsWebResource(symbolic.getAbsolutePath());
//        war.setWebXML("symbolic-enabled-web.xml");

        return war;
    }

    @Test
    @OperateOnDeployment("symbolic-enabled.war")
    public void testSymlinkingEnabled() {


    }

}
