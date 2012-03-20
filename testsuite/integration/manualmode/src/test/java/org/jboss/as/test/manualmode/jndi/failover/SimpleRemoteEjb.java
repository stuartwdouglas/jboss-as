package org.jboss.as.test.manualmode.jndi.failover;

import javax.annotation.Resource;
import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class SimpleRemoteEjb implements SimpleRemoteInterface {

    @Resource(name = "message")
    private String message;

    @Override
    public String getMessage() {
        return message;
    }
}
