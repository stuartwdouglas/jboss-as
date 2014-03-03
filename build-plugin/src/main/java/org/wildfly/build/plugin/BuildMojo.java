/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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

package org.wildfly.build.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.wildfly.build.plugin.model.Build;
import org.wildfly.build.plugin.model.BuildModelParser;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Stuart Douglas
 */

@Mojo(name = "build", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class BuildMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Commands to run before the deployment
     */
    @Parameter(alias = "config-file", required = true)
    private String configFile;

    @Parameter(defaultValue = "${basedir}", alias = "config-dir")
    private File configDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        FileInputStream configStream = null;
        try {
            configStream = new FileInputStream(new File(configDir, configFile));
            final Build build = new BuildModelParser(project.getProperties()).parse(configStream);


            Path moduleDirectory = Paths.get("/Users/stuart/workspace/wildfly/build/src/main/resources/modules/system/layers/base");
            getLog().info("RUNNING FOR " + moduleDirectory);
            ModuleUtils.enumerateModuleDirectory(getLog(), moduleDirectory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(configStream);
        }
    }

    private void safeClose(final Closeable ... closeable) {
        for(Closeable c : closeable) {
            if(c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    getLog().error("Failed to close resource", e);
                }
            }
        }
    }
}
