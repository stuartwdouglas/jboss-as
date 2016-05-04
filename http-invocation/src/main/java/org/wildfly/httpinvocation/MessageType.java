package org.wildfly.httpinvocation;

/**
 * @author Stuart Douglas
 */
public class MessageType {

    private final String type;
    private final int version;

    public MessageType(String type, int version) {
        this.type = type;
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageType that = (MessageType) o;

        if (version != that.version) return false;
        return type != null ? type.equals(that.type) : that.type == null;

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + version;
        return result;
    }
}
