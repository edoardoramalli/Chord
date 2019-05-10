package network.message;

import java.io.IOException;
import java.io.Serializable;

public class FindKeyResponse implements Message,Serializable {
    private Object value;
    private Long lockId;

    public FindKeyResponse(Object value, Long lockId) {
        this.value = value;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }

    public Object getValue() {
        return value;
    }
}
