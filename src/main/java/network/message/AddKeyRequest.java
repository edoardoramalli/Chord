package network.message;

import node.NodeInterface;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class AddKeyRequest implements Message, Serializable {
    private Map.Entry<Long, Object> keyValue;
    private Long lockId;

    public AddKeyRequest(Map.Entry<Long, Object> keyValue, Long lockId) {
        this.keyValue = keyValue;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Map.Entry<Long, Object> getKeyValue() {
        return keyValue;
    }

    public Long getLockId() {
        return lockId;
    }

}
