/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cmp.jdbc;

import javax.ejb.EJBException;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.jdbc.bridge.CMRInvocation;
import org.jboss.as.cmp.jdbc.bridge.CMRMessage;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.ejb3.context.spi.InvocationContext;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

/**
 * The role of this interceptor relationship messages from a related CMR field
 * and invoke the specified message on this container's cmr field of the
 * relationship.  This interceptor also manages the relation table data.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class JDBCRelationInterceptor implements Interceptor {
    // Attributes ----------------------------------------------------

    private CmpEntityBeanComponent component;

    /**
     * The log.
     */
    private Logger log;

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    // Public --------------------------------------------------------
    public void setComponent(CmpEntityBeanComponent component) {
        this.component = component;
        if (component != null) {
            log = Logger.getLogger(this.getClass().getName() + "." + component.getComponentName());
        }
    }

    public CmpEntityBeanComponent getComponent() {
        return component;
    }

    // Interceptor implementation --------------------------------------


    public Object processInvocation(final InterceptorContext interceptorContext) throws Exception {
        final CMRInvocation cmrInvocation = interceptorContext.getPrivateData(CMRInvocation.class);
        if (cmrInvocation == null) {
            return interceptorContext.proceed();
        }

        CMRMessage relationshipMessage = cmrInvocation.getCmrMessage();
        if (relationshipMessage == null) {
            // Not a relationship message. Invoke down the chain
            return interceptorContext.proceed();
        }

        // We are going to work with the context a lot
        final InvocationContext invocationContext = interceptorContext.getPrivateData(InvocationContext.class);
        CmpEntityBeanContext ctx = (CmpEntityBeanContext) invocationContext.getEJBContext();
        JDBCCMRFieldBridge cmrField = (JDBCCMRFieldBridge) interceptorContext.getParameters()[0];

        if (CMRMessage.GET_RELATED_ID == relationshipMessage) {
            // call getRelateId
            if (log.isTraceEnabled()) {
                log.trace("Getting related id: field=" + cmrField.getFieldName() + " id=" + ctx.getPrimaryKey());
            }
            return cmrField.getRelatedId(ctx);

        } else if (CMRMessage.ADD_RELATION == relationshipMessage) {
            // call addRelation
            Object relatedId = interceptorContext.getParameters()[1];
            if (log.isTraceEnabled()) {
                log.trace("Add relation: field=" + cmrField.getFieldName() +
                        " id=" + ctx.getPrimaryKey() +
                        " relatedId=" + relatedId);
            }

            cmrField.addRelation(ctx, relatedId);

            return null;

        } else if (CMRMessage.REMOVE_RELATION == relationshipMessage) {
            // call removeRelation
            Object relatedId = interceptorContext.getParameters()[1];
            if (log.isTraceEnabled()) {
                log.trace("Remove relation: field=" + cmrField.getFieldName() +
                        " id=" + ctx.getPrimaryKey() +
                        " relatedId=" + relatedId);
            }

            cmrField.removeRelation(ctx, relatedId);

            return null;
        } else if (CMRMessage.SCHEDULE_FOR_CASCADE_DELETE == relationshipMessage) {
            JDBCEntityBridge entity = (JDBCEntityBridge) cmrField.getEntity();
            entity.scheduleForCascadeDelete(ctx);
            return null;
        } else if (CMRMessage.SCHEDULE_FOR_BATCH_CASCADE_DELETE == relationshipMessage) {
            JDBCEntityBridge entity = (JDBCEntityBridge) cmrField.getEntity();
            entity.scheduleForBatchCascadeDelete(ctx);
            return null;
        } else {
            // this should not be possible we are using a type safe enum
            throw new EJBException("Unknown cmp2.0-relationship-message=" +
                    relationshipMessage);
        }
    }
}

