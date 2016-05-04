package org.wildfly.httpinvocation;

import io.undertow.server.HttpServerExchange;

import java.io.IOException;

/**
 * @author Stuart Douglas
 */
public interface HttpMessageHandler {

    void handle(HttpServerExchange exchange, RequestMessage message) throws IOException;

}
