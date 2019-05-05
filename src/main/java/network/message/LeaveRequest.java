package network.message;

import node.NodeInterface;

import java.io.IOException;
import java.io.Serializable;

public class LeaveRequest implements Message, Serializable {
    private Long nodeId;
    private NodeInterface newNode;
    private Long lockId;

    public LeaveRequest(Long nodeId, NodeInterface newNode, Long lockId) {
        this.nodeId = nodeId;
        this.newNode = newNode;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getNodeId() {
        return nodeId;
    }

    public NodeInterface getNewNode() {
        return newNode;
    }

    public Long getLockId() {
        return lockId;
    }
}
