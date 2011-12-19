package org.jboss.as.appclient.service;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.identity.Identity;
import org.jboss.security.identity.extensions.CredentialIdentity;

import static java.security.AccessController.doPrivileged;

/**
 * The default callback handler used by the
 *
 * @author Stuart Douglas
 */
public class DefaultApplicationClientCallbackHandler implements CallbackHandler {

    public static final String ANONYMOUS = "anonymous";

    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        final SecurityContext context = doPrivileged(securityContext());
        if (context == null) {
            //we have no security context, so we try and authenticate using the name anonymous
            for (final Callback current : callbacks) {
                if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName(ANONYMOUS);
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        } else {
            for (final Callback current : callbacks) {
                if (current instanceof NameCallback) {
                    final NameCallback ncb = (NameCallback) current;
                    final Set<Identity> identities = context.getSubjectInfo().getIdentities();
                    if (identities.isEmpty()) {
                        ncb.setName(ANONYMOUS);
                    } else {
                        final Identity identity = identities.iterator().next();
                        ncb.setName(identity.getName());
                    }
                } else if (current instanceof PasswordCallback) {
                    final PasswordCallback pcb = (PasswordCallback) current;
                    final Set<Identity> identities = context.getSubjectInfo().getIdentities();
                    if (identities.isEmpty()) {
                        throw new UnsupportedCallbackException(current);
                    } else {
                        final Identity identity = identities.iterator().next();
                        if (identity instanceof CredentialIdentity) {
                            pcb.setPassword((char[]) ((CredentialIdentity) identity).getCredential());
                        } else {
                            throw new UnsupportedCallbackException(current);
                        }
                    }
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }

        }
    }


    private static PrivilegedAction<SecurityContext> securityContext() {
        return new PrivilegedAction<SecurityContext>() {
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        };
    }
}
