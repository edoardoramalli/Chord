package network.message;

import java.io.IOException;
import java.io.Serializable;

public class FindKeyResponse implements Message,Serializable {
    private Long lockId;
    private Object value;

    public FindKeyResponse(Long lockId, Object value) {
        this.lockId = lockId;
        this.value = value;
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
