package org.jboss.as.test.integration.deployment.structure.war.symbolic;

import junit.framework.Assert;
import org.apache.commons.lang.SystemUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.web.deployment.WarDeploymentProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author navssurtani
 */
@RunWith(Arquillian.class)
public class SymbolicLinkingUnitTestCase {

    private static final Logger log = Logger.getLogger(SymbolicLinkingUnitTestCase.class);
    private final String WAR_NAME = "explodedDeployment.war";

    private File warDeployment = null;
    private static File symbolic = null;

    @Before
    public void setUpDeployment() {
        // First check if the exploded directory exists.
        Assert.assertTrue(checkForDeployment());

        // Create the symbolic link.
        createSymbolicLink();

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        operation.get(ModelDescriptionConstants.OP_ADDR).add(ModelDescriptionConstants.DEPLOYMENT, warDeployment.getName());

        final ModelNode content = new ModelNode();
        content.get(ModelDescriptionConstants.ARCHIVE).set(warDeployment.getName());
        content.get(ModelDescriptionConstants.PATH).set(warDeployment.getAbsolutePath());

        operation.get(ModelDescriptionConstants.ENABLE).set(true);
    }

    @AfterClass
    public static void cleanUpLinks() {
        // Only need to do this if the symbolic link is not null.
        if (symbolic != null) {
            log.info("Attempting to delete " + symbolic.getAbsolutePath() + " with outcome: " + symbolic.delete());
        }
    }

    @Test
    public void testEnabled() {
        WarDeploymentProcessor wdp = new WarDeploymentProcessor("default-host", true);


    }

    @Test
    public void testDisabled() {
        WarDeploymentProcessor wdp = new WarDeploymentProcessor("default-host", true);

    }

    private boolean checkForDeployment() {
        String warLocation = locateFromName();
        warDeployment = new File(warLocation);
        log.info("Checking to see if exploded deployment exists, at path: " + warDeployment.getAbsolutePath());
        return warDeployment.exists();
    }

    //TODO: Run on Windows to come up with appropriate 'hack' based off the system used.

    private String locateFromName() {
        // Find the deployment from the classpath. This is a massive, massive hack but this is being done for want of
        // a more efficient approach. Involves lots of cutting and chopping of Strings.
        String preSubString = null, postSubString = null;

        try {
            String path = this.getClass().getResource(WAR_NAME).toURI().toString();

            // From running this before, it appears that the String looks like:
            // file:/{$jboss.testsuite.basic}/target/test-classes/org/jboss/as/test/integration/deployment
            // /structure/war/symbolic/explodedDeployment.war

            // We really want to point at:
            // /{$jboss.testsuite.basic}/src/test/resources/deployment/structure/war/symbolic/explodedDeployment.war

            // Get the substring path which ends at the basic directory:
            preSubString = path.substring(path.indexOf("/"), path.indexOf("target"));
            log.debug("Pre-substring: " + preSubString);

            // Get the second part of the substring that we want to keep. This is the part containing the package name.
            postSubString = path.substring(path.indexOf("deployment"));
            log.debug("Post-substring" + postSubString);
        } catch (URISyntaxException e) {
            log.fatal("Error with converting URL to URI.", e);
        }
        return preSubString + "src/test/resources/" + postSubString;
    }

    private void createSymbolicLink() {
        // We know that the real index.html file is in the directory one above where the .war file is.
        File index = new File(warDeployment.getParent(), "index.html");
        log.info("Path to index.html is: " + index.getAbsolutePath());
        Assert.assertTrue(index.exists());

        // Now create the link based off the operating system:
        String toExecute;
        if (SystemUtils.IS_OS_WINDOWS) {
            // Windows implementation
            log.info("Windows based OS detected.");
            toExecute = "mklink \\D " + index.getAbsolutePath() + " " + warDeployment.getAbsolutePath()
                                + "\\symbolic.html";

        } else {

            // Linux/UNIX implementation
            log.info("Non-windows based OS detected.");
            toExecute = "ln -s " + index.getAbsolutePath() + " " + warDeployment.getAbsolutePath() + "/symbolic.html";
        }
        try {
            // Executing the String using the Runtime class.
            log.info("String to execute is: " + toExecute);
            Runtime.getRuntime().exec(toExecute);
            symbolic = new File(warDeployment.getAbsolutePath(), "symbolic.html");
        } catch (IOException e) {
            log.fatal("Caught IOException while trying to execute the following String to create a symbolic link: " +
                              toExecute, e);
        }
        Assert.assertTrue(symbolic.exists());
    }

}
