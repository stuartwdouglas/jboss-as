package org.jboss.as.test.integration.ee.injection.resource.datasource;

import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DataSourceMappingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackage(DataSourceMappingTestCase.class.getPackage())
                .addAsWebInfResource(DataSourceMappingTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

    }

    @ArquillianResource
    private InitialContext initialContext;

    @Test
    public void testDataSourceLookup() throws NamingException, SQLException {
        DataSource dataSource = (DataSource) initialContext.lookup("java:comp/env/jdbc/myds");
        dataSource.getConnection().createStatement().execute("select 1");

    }


}
