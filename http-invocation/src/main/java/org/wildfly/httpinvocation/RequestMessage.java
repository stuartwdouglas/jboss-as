package org.wildfly.httpinvocation;

import java.util.List;

/**
 * @author Stuart Douglas
 */
public class RequestMessage {
    private final MessageType type;
    private final List<MessageType> responseTypes;

    public RequestMessage(MessageType type, List<MessageType> responseTypes) {
        this.type = type;
        this.responseTypes = responseTypes;
    }

    public MessageType getType() {
        return type;
    }

    public List<MessageType> getResponseTypes() {
        return responseTypes;
    }
}
