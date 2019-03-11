package network.message;

import node.NodeInterface;

import java.io.IOException;
import java.io.Serializable;

public class GetPredecessorRequest implements Message, Serializable {
    private Long lockId;

    public GetPredecessorRequest(Long lockId) {
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
