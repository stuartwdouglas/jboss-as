/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.bridge;

import java.security.Principal;
import javax.transaction.Transaction;
import org.jboss.invocation.InvocationType;

/**
 * Optimized invocation object for local CMR invocations
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: 81030 $
 */
public final class CMRInvocation {
    private Object id;
    private Entrancy entrancy;
    private CMRMessage cmrMessage;
    private Transaction transaction;
    private Principal principal;
    private Object credential;
    private InvocationType type;

    public CMRInvocation() {
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Entrancy getEntrancy() {
        return entrancy;
    }

    public void setEntrancy(Entrancy entrancy) {
        this.entrancy = entrancy;
    }

    public CMRMessage getCmrMessage() {
        return cmrMessage;
    }

    public void setCmrMessage(CMRMessage cmrMessage) {
        this.cmrMessage = cmrMessage;
    }


    public void setArguments(Object[] objects) {

    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public void setCredential(Object credential) {
        this.credential = credential;
    }


    public void setType(InvocationType type) {
        this.type = type;
    }
}
