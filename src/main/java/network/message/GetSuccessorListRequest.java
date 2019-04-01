package network.message;

import java.io.IOException;
import java.io.Serializable;

public class GetSuccessorListRequest implements Message, Serializable {
    private Long lockId;

    public GetSuccessorListRequest(Long lockId) {
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }
}
