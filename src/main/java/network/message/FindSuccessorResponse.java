package network.message;

import node.NodeInterface;

import java.io.IOException;
import java.io.Serializable;

public class FindSuccessorResponse implements Message, Serializable {
    private NodeInterface node;
    private Long lockId;

    public FindSuccessorResponse(NodeInterface node, Long lockId) {
        this.node = node;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }

    public NodeInterface getNode() {
        return node;
    }
}
