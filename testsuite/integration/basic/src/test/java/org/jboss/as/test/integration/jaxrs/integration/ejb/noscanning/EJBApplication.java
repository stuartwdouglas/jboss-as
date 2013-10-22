package org.jboss.as.test.integration.jaxrs.integration.ejb.noscanning;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
@ApplicationPath("/myjaxrs")
public class EJBApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.<Class<?>>singleton(EJBResource.class);
    }
}
