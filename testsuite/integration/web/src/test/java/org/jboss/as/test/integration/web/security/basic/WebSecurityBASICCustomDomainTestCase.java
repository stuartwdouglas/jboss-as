/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security.basic;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.integration.web.security.jaspi.WebSecurityJaspiTestCase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * Unit Test the BASIC authentication
 *
 * @author Anil Saldhana
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebSecurityBASICCustomDomainTestCase.BasicSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class WebSecurityBASICCustomDomainTestCase {

    static class BasicSecurityDomainSetup implements ServerSetupTask {
        static final Path WILDFLY_HOME = Paths.get(System.getProperty("jbossRunHome"));
        public static final String APPLICATION_ROLE = "gooduser";
        public static final String SECURITY_DOMAIN = "basic-application-security-domain";
        public static final String AUTH_METHOD = "BASIC";
        public static final String APPLICATION_USER = "testUser";
        public static final String APPLICATION_PASSWORD = "password+";

        private static final String HTTPS_HOST = "https://localhost:8443";

        private PathAddress domainAddress;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try {
                // Force WildFly to create the default application.keystore
                try (CloseableHttpClient httpclient = HttpClients.custom()
                        .build()) {
                    HttpGet httpget = new HttpGet(HTTPS_HOST);
                    httpclient.execute(httpget);
                } catch (Exception ignored) {
                }

                UserManager.addApplicationUser(APPLICATION_USER, APPLICATION_PASSWORD, WILDFLY_HOME);
                UserManager.addRoleToApplicationUser(APPLICATION_USER, APPLICATION_ROLE, WILDFLY_HOME);

                domainAddress = PathAddress.pathAddress().append("subsystem", "undertow")
                        .append("application-security-domain", SECURITY_DOMAIN);

                final ModelNode compositeOp = new ModelNode();
                compositeOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.COMPOSITE);
                compositeOp.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();

                ModelNode steps = compositeOp.get(ModelDescriptionConstants.STEPS);

                // /subsystem=elytron/security-domain=EjbDomain:add(default-realm=UsersRoles, realms=[{realm=UsersRoles}])
                ModelNode addDomain = Util.createAddOperation(domainAddress);
                addDomain.get("http-authentication-factory").set("application-http-authentication");
                steps.add(addDomain);

                applyUpdate(managementClient.getControllerClient(), compositeOp, false);

                ModelNode operation = new ModelNode();
                operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
                operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
                operation.get(ModelDescriptionConstants.NAME).set("server-state");
                applyUpdate(managementClient.getControllerClient(), operation, false);


                //executeReloadAndWaitForCompletion(managementClient.getControllerClient());

            } catch (Exception e) {
                System.out.println("*** BasicSecurityDomainSetup.setup() err");
                e.printStackTrace();
            }
            System.out.println("*** BasicSecurityDomainSetup.setup() end");
        }

        public static void executeReload(ModelControllerClient client, boolean adminOnly) {
            ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
            operation.get(ModelDescriptionConstants.OP).set("reload");
            operation.get("admin-only").set(adminOnly);

            executeReload(client, operation);
        }

        /** Default time, in ms, to wait for reload to complete. */
        public static final int TIMEOUT = 100000;

        /**
         * Executes a {@code reload} operation and waits the {@link #TIMEOUT default timeout} for the reload to complete.
         *
         * @param client
         *            the client to use for the request. Cannot be {@code null}
         *
         * @throws AssertionError
         *             if the reload does not complete within the timeout
         */
        public static void executeReloadAndWaitForCompletion(ModelControllerClient client) {
            executeReloadAndWaitForCompletion(client, TIMEOUT);
        }

        /**
         * Executes a {@code reload} operation and waits a configurable maximum time for the reload to complete.
         *
         * @param client
         *            the client to use for the request. Cannot be {@code null}
         * @param timeout
         *            maximum time to wait for the reload to complete, in milliseconds
         *
         * @throws AssertionError
         *             if the reload does not complete within the specified timeout
         */
        public static void executeReloadAndWaitForCompletion(ModelControllerClient client, int timeout) {
            executeReloadAndWaitForCompletion(client, timeout, false, null, -1);
        }

        /**
         * @return The server address of node0
         */
        public static String getServerAddress() {
            String address = System.getProperty("management.address");
            if (address == null) {
                address = System.getProperty("node0");
            }
            if (address != null) {
                return formatPossibleIpv6Address(address);
            }
            return "localhost";
        }

        /**
         * @return The server port for node0
         */
        public static int getServerPort() {
            // this here is just fallback logic for older testsuite code that wasn't updated to newer property names
            return Integer.getInteger("management.port", Integer.getInteger("as.managementPort", 9990));
        }

        public static String formatPossibleIpv6Address(String address) {
            if (address == null) {
                return address;
            }
            if (!address.contains(":")) {
                return address;
            }
            if (address.startsWith("[") && address.endsWith("]")) {
                return address;
            }
            return "[" + address + "]";
        }

        /**
         * Executes a {@code reload} operation, optionally putting the server into {@code admin-only} running mode, and
         * waits a configurable maximum time for the reload to complete.
         *
         * @param client
         *            the client to use for the request. Cannot be {@code null}
         * @param timeout
         *            maximum time to wait for the reload to complete, in milliseconds
         * @param adminOnly
         *            if {@code true}, the server will be reloaded in admin-only mode
         * @param serverAddress
         *            if {@code null}, use {@code TestSuiteEnvironment.getServerAddress()} to create the
         *            ModelControllerClient
         * @param serverPort
         *            if {@code -1}, use {@code TestSuiteEnvironment.getServerPort()} to create the ModelControllerClient
         *
         * @throws AssertionError
         *             if the reload does not complete within the specified timeout
         */
        public static void executeReloadAndWaitForCompletion(ModelControllerClient client, int timeout, boolean adminOnly,
                String serverAddress, int serverPort) {
            executeReload(client, adminOnly);
            waitForLiveServerToReload(timeout, "remote+http", serverAddress != null ? serverAddress : getServerAddress(),
                    serverPort != -1 ? serverPort : getServerPort());
        }

        private static void waitForLiveServerToReload(int timeout, String protocol, String serverAddress, int serverPort) {
            long start = System.currentTimeMillis();
            ModelNode operation = new ModelNode();
            operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
            operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
            operation.get(ModelDescriptionConstants.NAME).set("server-state");
            while (System.currentTimeMillis() - start < timeout) {
                // do the sleep before we check, as the attribute state may not change instantly
                // also reload generally takes longer than 100ms anyway
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                try {
                    ModelControllerClient liveClient = ModelControllerClient.Factory.create(protocol, serverAddress,
                            serverPort);
                    try {
                        ModelNode result = liveClient.execute(operation);
                        if ("running".equals(result.get(ModelDescriptionConstants.RESULT).asString())) {
                            return;
                        }
                    } catch (IOException e) {
                    } finally {
                        IoUtils.safeClose(liveClient);
                    }
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
            Assert.fail("Live Server did not reload in the imparted time.");
        }

        public static void executeReload(ModelControllerClient client, ModelNode reloadOp) {
            try {
                ModelNode result = client.execute(reloadOp);
                Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ClientConstants.OUTCOME).asString());
            } catch (IOException e) {
                final Throwable cause = e.getCause();
                if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                    throw new RuntimeException(e);
                } // else ignore, this might happen if the channel gets closed before we got the response
            }
        }

        static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure)
                throws IOException {
            ModelNode result = client.execute(new OperationBuilder(update).build());
            System.out.println("management op result = "+ result);
            if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
                return;
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // URL cliUrl = this.getClass().getClassLoader().getResource("security/cli/basic/tear-down.cli");
            // wildFlyCli.run(cliUrl).assertSuccess();

            UserManager.removeApplicationUser(APPLICATION_USER, WILDFLY_HOME);
            UserManager.revokeRoleFromApplicationUser(APPLICATION_USER, APPLICATION_ROLE, WILDFLY_HOME);
        }
    }

    static class UserManager {

        private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

        private UserManager() {
            // Hide constructor for utility class
        }

        public static void addApplicationUser(String userName, String password, Path jbossHome) {
            System.out.println("*** props = "+ jbossHome.resolve("standalone/configuration/application-users.properties"));
            addUser(userName, "ApplicationRealm", password, jbossHome.resolve("standalone/configuration/application-users.properties"));
        }

        public static void addManagementUser(String userName, String password, Path jbossHome) {
            addUser(userName, "ManagementRealm", password, jbossHome.resolve("standalone/configuration/mgmt-users.properties"));
        }

        private static void addUser(String userName, String realm, String password, Path propertiesFile) {
            Properties properties = readPropertiesFile(propertiesFile);
            System.out.println("*** props = "+ properties);
            properties.put(userName, encryptPassword(userName, password, realm));
            writePropertiesFile(properties, propertiesFile);
        }

        public static void addRoleToApplicationUser(String userName, String role, Path jbossHome) {
            addRoleToUser(userName, role, jbossHome.resolve("standalone/configuration/application-roles.properties"));
        }

        public static void addRoleToManagementUser(String userName, String role, Path jbossHome) {
            addRoleToUser(userName, role, jbossHome.resolve("standalone/configuration/mgmt-roles.properties"));
        }

        private static void addRoleToUser(String userName, String role, Path propertiesFile) {
            Properties properties = readPropertiesFile(propertiesFile);
            properties.put(userName, role);
            writePropertiesFile(properties, propertiesFile);
        }

        public static void removeApplicationUser(String userName, Path jbossHome) {
            removeUser(userName, jbossHome.resolve("standalone/configuration/application-users.properties"));
        }

        public static void removeManagementUser(String userName, Path jbossHome) {
            removeUser(userName, jbossHome.resolve("standalone/configuration/mgmt-users.properties"));
        }

        private static void removeUser(String userName, Path propertiesFile) {
            Properties properties = readPropertiesFile(propertiesFile);
            properties.remove(userName);
            writePropertiesFile(properties, propertiesFile);
        }

        public static void revokeRoleFromApplicationUser(String userName, String role, Path jbossHome) {
            revokeRoleFromUser(userName, role, jbossHome.resolve("standalone/configuration/application-roles.properties"));
        }

        public static void revokeRoleFromManagementUser(String userName, String role, Path jbossHome) {
            revokeRoleFromUser(userName, role, jbossHome.resolve("standalone/configuration/mgmt-roles.properties"));
        }

        private static void revokeRoleFromUser(String userName, String role, Path propertiesFile) {
            Properties properties = readPropertiesFile(propertiesFile);
            String roles = properties.getProperty(userName);
            if (roles != null && ! roles.isEmpty()) {
                List<String> roleList = new ArrayList<>(Arrays.asList(roles.split(",")));
                roleList.remove(role);
                if (roleList.isEmpty()) {
                    properties.remove(userName);
                } else {
                    String updatedRoles = roleList.stream().collect(Collectors.joining(","));
                    properties.put(userName, updatedRoles);
                }
                properties.put(userName, role);
                writePropertiesFile(properties, propertiesFile);
            }
        }

        private static Properties readPropertiesFile(Path file) {
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return properties;
        }

        private static void writePropertiesFile(Properties properties, Path file) {
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, null);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private static String encryptPassword(String userName, String password, String realm) {
            try {
                String stringToEncrypt = String.format("%s:%s:%s", userName, realm, password);

                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hashedPassword = md.digest(stringToEncrypt.getBytes(StandardCharsets.UTF_8));

                char[] converted = new char[hashedPassword.length * 2];
                for (int i = 0; i < hashedPassword.length; i++) {
                    byte b = hashedPassword[i];
                    converted[i * 2] = HEX_CHARS[b >> 4 & 0x0F];
                    converted[i * 2 + 1] = HEX_CHARS[b & 0x0F];
                }
                return String.valueOf(converted);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final Logger log = Logger.getLogger(WebSecurityJaspiTestCase.class);

    private static final String JBOSS_WEB_CONTENT = "<?xml version=\"1.0\"?>\n" +
            "<jboss-web>\n" +
            "    <security-domain>" + BasicSecurityDomainSetup.SECURITY_DOMAIN + "</security-domain>\n" +
            "</jboss-web>";

    @Deployment
    public static WebArchive deployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure-basic-custom-domain.war");
        war.addClass(SecuredServlet.class);

        war.addAsWebInfResource(new StringAsset(JBOSS_WEB_CONTENT), "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityBASICCustomDomainTestCase.class.getPackage(), "web.xml", "web.xml");

        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void basic() throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                new UsernamePasswordCredentials(BasicSecurityDomainSetup.APPLICATION_USER, BasicSecurityDomainSetup.APPLICATION_PASSWORD));
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {

            HttpGet httpget = new HttpGet(url.toExternalForm() + "secured/");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            if (entity != null) {
                log.trace("Response content length: " + entity.getContentLength());
            }
            assertEquals(200, statusLine.getStatusCode());

            EntityUtils.consume(entity);
        }
    }

    @Test
    public void anonymous() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .build()) {

            HttpGet httpget = new HttpGet(url.toExternalForm() + "secured/");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            if (entity != null) {
                log.trace("Response content length: " + entity.getContentLength());
            }
            assertEquals(401, statusLine.getStatusCode());

            EntityUtils.consume(entity);
        }
    }

}