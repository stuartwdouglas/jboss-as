/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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


package org.jboss.as.test.integration.web.symbolic;

import org.apache.commons.lang.SystemUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Abstract class containing setup methods used by {@link SymlinkingEnabledUnitTestCase} and {@link SymlinkingDisabledUnitTestCase} <br>
 * <p/>
 * This will create the symbolic link based off the O/S being used, which would in turn be used by the sub-classes.
 *
 * @author navssurtani
 */

public abstract class AbstractSymbolicLinkingUnitTestCase {

    private static Logger log = Logger.getLogger(AbstractSymbolicLinkingUnitTestCase.class);

    private static final File tempDir = new File(System.getProperty("java.io.tmpdir"));

    // Has to be static so that Arquillian won't break.
    protected static File symbolic = null;

    @ArquillianResource
    protected URL url;

    //TODO: Once the whole of AS 7 has been converted to be using Java 7, then we should use the Files native call.
    protected static File getSymbolicLink() throws IOException {
        File index = new File("index.html");
        if (!index.exists()) throw new IOException("Base index file does not exist.");

        // Based off the system type that is running, we will return an index.html file which is really a symbolic link
        // to some other file elsewhere on the system.
        Runtime runtime = Runtime.getRuntime();
        String toExecute;

        if (SystemUtils.IS_OS_WINDOWS) {

            // Windows implementation
            log.info("Windows based OS detected.");
            toExecute = "mklink \\D " + index.getAbsolutePath() + " " + tempDir.getAbsolutePath() + "\\symbolic.html";

        } else {

            // Linux/UNIX implementation
            log.info("Non-windows based OS detected.");
            toExecute = "ln -s " + index.getAbsolutePath() + " " + tempDir.getAbsolutePath() + "/symbolic.html";

        }
        log.info("The String to execute is: " + toExecute);
        runtime.exec(toExecute);

        return new File(tempDir, "symbolic.html");
    }
}
