package network.message;

import java.io.IOException;
import java.io.Serializable;

public class GetNodeIdResponse implements Message, Serializable {
    private Long nodeId;
    private Long lockId;

    public GetNodeIdResponse(Long nodeId, Long lockId) {
        this.nodeId = nodeId;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getNodeId() {
        return nodeId;
    }

    public Long getLockId() {
        return lockId;
    }
}
