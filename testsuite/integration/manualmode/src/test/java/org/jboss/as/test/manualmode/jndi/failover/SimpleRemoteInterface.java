package org.jboss.as.test.manualmode.jndi.failover;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface SimpleRemoteInterface {

    String getMessage();
}
