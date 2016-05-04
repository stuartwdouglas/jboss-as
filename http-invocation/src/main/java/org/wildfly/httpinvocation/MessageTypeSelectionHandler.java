package org.wildfly.httpinvocation;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.jboss.marshalling.Marshaller;
import org.wildfly.httpinvocation.logging.HttpInvocationLogger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Htt
 *
 * @author Stuart Douglas
 */
public class MessageTypeSelectionHandler implements HttpHandler {

    public static final String VERSION = "version";

    private final Map<String, Holder> messageHandlers;
    private final Function<OutputStream, Marshaller> marshallerSupplier;
    private final String exceptionMessageType;

    public MessageTypeSelectionHandler(Function<OutputStream, Marshaller> marshallerSupplier, String exceptionMessageType) {
        this.marshallerSupplier = marshallerSupplier;
        this.exceptionMessageType = exceptionMessageType;
        this.messageHandlers = new ConcurrentHashMap<>();
    }

    public void register(String messageType, HttpString method, HttpMessageHandler handler) {
        messageHandlers.put(messageType, new Holder(handler, method));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        exchange.startBlocking();
        String contentType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
        if(contentType == null) {
            sendError(exchange, HttpInvocationLogger.ROOT_LOGGER.missingHeader(Headers.CONTENT_TYPE_STRING), StatusCodes.BAD_REQUEST);
            return;
        }

        MessageType mt = parseMessageType(exchange, contentType, HttpInvocationLogger.ROOT_LOGGER::invalidContentType);
        if(mt == null) {
            return;
        }

        String accept = exchange.getRequestHeaders().getFirst(Headers.ACCEPT);
        if(accept == null) {
            sendError(exchange, HttpInvocationLogger.ROOT_LOGGER.missingHeader(Headers.ACCEPT_STRING), StatusCodes.BAD_REQUEST);
            return;
        }
        String[] parts = accept.split(",");
        List<MessageType> acceptMessages = new ArrayList<>(parts.length);
        for(String p : parts) {
            MessageType acceptType = parseMessageType(exchange, p, HttpInvocationLogger.ROOT_LOGGER::invalidAcceptHeader);
            if(acceptType == null) {
                return;
            }
            acceptMessages.add(acceptType);
        }
        RequestMessage message = new RequestMessage(mt, acceptMessages);

        Holder holder = messageHandlers.get(message.getType().getType());

        if(holder == null) {
            sendError(exchange, HttpInvocationLogger.ROOT_LOGGER.invalidContentType(message.getType().getType()), StatusCodes.BAD_REQUEST);
            return;
        }
        if(!holder.method.equals(exchange.getRequestMethod())) {
            sendError(exchange, null, StatusCodes.METHOD_NOT_ALLOWED);
            return;
        }
        HttpMessageHandler handler = holder.handler;
        try {
            handler.handle(exchange, message);
        } catch (RuntimeException e) {
            //this should only happen if there is a problem with the request, all other errors should be written
            //out in a way that is relevant to the message
            sendError(exchange, e, StatusCodes.BAD_REQUEST);
        }
    }

    private MessageType parseMessageType(HttpServerExchange exchange, String contentType, Function<String, RuntimeException> errorProducer) throws IOException {
        int pos = contentType.indexOf(";");
        if(pos == -1) {
            sendError(exchange, errorProducer.apply(contentType), StatusCodes.BAD_REQUEST);
            return null;
        }
        String type = contentType.substring(0, pos);
        String versionString = Headers.extractTokenFromHeader(contentType, VERSION);
        if(versionString == null) {
            sendError(exchange, errorProducer.apply(contentType), StatusCodes.BAD_REQUEST);
            return null;
        }
        int version;
        try {
            version = Integer.parseInt(versionString);
        } catch (NumberFormatException e) {
            sendError(exchange, errorProducer.apply(contentType), StatusCodes.BAD_REQUEST);
            return null;
        }
        return new MessageType(type, version);
    }

    private void sendError(HttpServerExchange exchange, Throwable t, int status) throws IOException {
        exchange.setStatusCode(status);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, exceptionMessageType);
        try (DataOutputStream outputStream = new DataOutputStream(exchange.getOutputStream())) {
            // write out the exception
            final Marshaller marshaller = marshallerSupplier.apply(outputStream);
            marshaller.writeObject(t);
            // finish marshalling
            marshaller.finish();
        }
        exchange.endExchange();
    }

    private final class Holder {
        final HttpMessageHandler handler;
        final HttpString method;

        private Holder(HttpMessageHandler handler, HttpString method) {
            this.handler = handler;
            this.method = method;
        }
    }
}
