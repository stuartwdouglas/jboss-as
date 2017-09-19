package org.wildfly.extension.grpc;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Stuart Douglas
 */
@SuppressWarnings("deprecation")
@MessageLogger(projectCode = "WFLYGRPC", length = 4)
public interface GrpcLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    GrpcLogger ROOT_LOGGER = Logger.getMessageLogger(GrpcLogger.class, "org.wildfly.extension.grpc");

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 1, value = "Failed to load GRPC class %s")
    void failedToLoad(String s, @Cause Throwable t);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 2, value = "Unable to create GRPC endpooint from class %s, we were unable to determine the protobuf generated abstract superclass")
    void unableToProcess(Class<?> realClass);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 3, value = "Failed to install GRPC endpoint %s due to an error")
    void failedToInstall(Class<?> realClass, @Cause Exception e);
}
