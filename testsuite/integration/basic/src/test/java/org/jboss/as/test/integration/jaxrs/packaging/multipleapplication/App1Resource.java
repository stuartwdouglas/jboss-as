package org.jboss.as.test.integration.jaxrs.packaging.multipleapplication;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author Stuart Douglas
 */
@Path("resource")
@Produces({"text/plain"})
public class App1Resource {

    @GET
    public String getMessage() {
        return "RES1";
    }

}
