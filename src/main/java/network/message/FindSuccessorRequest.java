package network.message;

import node.NodeInterface;

import java.io.IOException;
import java.io.Serializable;

public class FindSuccessorRequest implements Message, Serializable {
    private NodeInterface node;
    private long id;
    private Long lockId;

    public FindSuccessorRequest(Long id, Long lockId) {
        this.id = id;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public long getId() {
        return id;
    }

    public Long getLockId() {
        return lockId;
    }
}
