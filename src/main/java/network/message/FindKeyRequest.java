package network.message;

import java.io.IOException;
import java.io.Serializable;

public class FindKeyRequest implements Message,Serializable {
    private Long lockId;
    private long key;

    public FindKeyRequest(Long lockId, long key) {
        this.lockId = lockId;
        this.key = key;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }

    public long getKey() {
        return key;
    }
}
