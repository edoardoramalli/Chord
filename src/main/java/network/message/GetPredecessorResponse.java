package network.message;

import node.NodeInterface;

import java.io.IOException;
import java.io.Serializable;

public class GetPredecessorResponse implements Message, Serializable {
    private NodeInterface node;
    private Long lockId;

    public GetPredecessorResponse(NodeInterface node, Long lockId) {
        this.node = node;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public NodeInterface getNode() {
        return node;
    }

    public Long getLockId() {
        return lockId;
    }
}
