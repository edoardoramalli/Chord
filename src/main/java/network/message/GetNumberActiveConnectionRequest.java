package network.message;

import java.io.IOException;
import java.io.Serializable;


public class GetNumberActiveConnectionRequest implements Message, Serializable {
    private Long lockId;
    private long id;

    public GetNumberActiveConnectionRequest(Long id, Long lockId) {
        this.lockId = lockId;
        this.id = id;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }

    public long getId() {
        return id;
    }
}
