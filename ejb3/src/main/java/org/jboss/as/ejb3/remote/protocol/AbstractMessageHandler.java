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

package org.jboss.as.ejb3.remote.protocol;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.protocol.versionone.ChannelAssociation;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.remoting3.MessageOutputStream;


/**
 * @author Jaikiran Pai
 */
public abstract class AbstractMessageHandler implements MessageHandler {

    protected static final byte HEADER_NO_SUCH_EJB_FAILURE = 0x0A;
    protected static final byte HEADER_NO_SUCH_EJB_METHOD_FAILURE = 0x0B;
    protected static final byte HEADER_SESSION_NOT_ACTIVE_FAILURE = 0x0C;
    private static final byte HEADER_INVOCATION_EXCEPTION = 0x06;


    protected void writeException(final ChannelAssociation channelAssociation, final MarshallerFactory marshallerFactory,
                                  final short invocationId, final Throwable t,
                                  final Map<String, Object> attachments) throws IOException {
        final DataOutputStream outputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
        }
        outputStream = new DataOutputStream(messageOutputStream);
        try {
            // write the header
            outputStream.write(HEADER_INVOCATION_EXCEPTION);
            // write the invocation id
            outputStream.writeShort(invocationId);
            // write out the exception
            final Marshaller marshaller = MarshallingSupport.prepareForMarshalling(marshallerFactory, outputStream);
            marshaller.writeObject(t);
            // write the attachments
            MarshallingSupport.writeAttachments(marshaller, attachments);
            // finish marshalling
            marshaller.finish();
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            outputStream.close();
        }
    }

    protected void writeInvocationFailure(final ChannelAssociation channelAssociation, final byte messageHeader, final short invocationId, final String failureMessage) throws IOException {
        final DataOutputStream dataOutputStream;
        final MessageOutputStream messageOutputStream;
        try {
            messageOutputStream = channelAssociation.acquireChannelMessageOutputStream();
        } catch (Exception e) {
            throw EjbLogger.ROOT_LOGGER.failedToOpenMessageOutputStream(e);
        }
        dataOutputStream = new DataOutputStream(messageOutputStream);
        try {
            // write header
            dataOutputStream.writeByte(messageHeader);
            // write invocation id
            dataOutputStream.writeShort(invocationId);
            // write the failure message
            dataOutputStream.writeUTF(failureMessage);
        } finally {
            channelAssociation.releaseChannelMessageOutputStream(messageOutputStream);
            dataOutputStream.close();
        }

    }

    protected void writeNoSuchEJBFailureMessage(final ChannelAssociation channelAssociation, final short invocationId, final String appName, final String moduleName,
                                                final String distinctname, final String beanName, final String viewClassName) throws IOException {
        final StringBuffer sb = new StringBuffer("No such EJB[");
        sb.append("appname=").append(appName).append(",");
        sb.append("modulename=").append(moduleName).append(",");
        sb.append("distinctname=").append(distinctname).append(",");
        sb.append("beanname=").append(beanName);
        if (viewClassName != null) {
            sb.append(",").append("viewclassname=").append(viewClassName);
        }
        sb.append("]");
        this.writeInvocationFailure(channelAssociation, HEADER_NO_SUCH_EJB_FAILURE, invocationId, sb.toString());
    }

    protected void writeNoSuchEJBMethodFailureMessage(final ChannelAssociation channelAssociation, final short invocationId, final String appName, final String moduleName,
                                                      final String distinctname, final String beanName, final String viewClassName,
                                                      final String methodName, final String[] methodParamTypes) throws IOException {
        final StringBuffer sb = new StringBuffer("No such method ");
        sb.append(methodName).append("(");
        if (methodParamTypes != null) {
            for (int i = 0; i < methodParamTypes.length; i++) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(methodParamTypes[i]);
            }
        }
        sb.append(") on EJB[");
        sb.append("appname=").append(appName).append(",");
        sb.append("modulename=").append(moduleName).append(",");
        sb.append("distinctname=").append(distinctname).append(",");
        sb.append("beanname=").append(beanName).append(",");
        sb.append("viewclassname=").append(viewClassName).append("]");
        this.writeInvocationFailure(channelAssociation, HEADER_NO_SUCH_EJB_METHOD_FAILURE, invocationId, sb.toString());
    }

}
