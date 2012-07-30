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

package org.jboss.as.test.integration.management.api.suspend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.Assert;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServerSuspendTestCase {

    @Deployment
    public static Archive<WebArchive> deploy() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClass(SuspendServlet.class);
    }

    @ArquillianResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    @Test
    public void testWebServerOperationsSuspend() throws URISyntaxException, IOException, InterruptedException {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url.toURI());
        HttpResponse result = client.execute(get);
        Assert.assertEquals(200, result.getStatusLine().getStatusCode());
        String responseMessage = EntityUtils.toString(result.getEntity());
        org.junit.Assert.assertEquals(SuspendServlet.MESSAGE, responseMessage);

        final ModelNode suspend = new ModelNode();
        suspend.get(ModelDescriptionConstants.OP).set("suspend");

        managementClient.getControllerClient().execute(suspend);

        //this permit manager should shut down

        result = client.execute(get);
        Assert.assertEquals(503, result.getStatusLine().getStatusCode());
        EntityUtils.toString(result.getEntity());

    }

}
