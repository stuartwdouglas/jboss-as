package org.wildfly.iiop.openjdk.csiv2;

import org.jboss.security.RunAs;
import org.jboss.security.SecurityContextAssociation;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;

/**
 * @author Stuart Douglas
 */
class SecurityActions {

    private static final PrivilegedAction<Principal> GET_PRINCIPLE_ACTION = () -> SecurityContextAssociation.getPrincipal();

    private static final PrivilegedAction<Object> GET_CREDENTIAL_ACTION = () -> SecurityContextAssociation.getCredential();

    private static final PrivilegedAction<RunAs> PEEK_RUN_AS_IDENTITY_ACTION = () -> SecurityContextAssociation.peekRunAsIdentity();

    static Principal getPrincipal() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(GET_PRINCIPLE_ACTION);
        } else {
            return SecurityContextAssociation.getPrincipal();
        }
    }

    static Object getCredential() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(GET_CREDENTIAL_ACTION);
        } else {
            return SecurityContextAssociation.getCredential();
        }
    }

    static RunAs peekRunAsIdentity() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(PEEK_RUN_AS_IDENTITY_ACTION);
        } else {
            return SecurityContextAssociation.peekRunAsIdentity();
        }
    }
}
