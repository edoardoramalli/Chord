package network.message;

import node.NodeInterface;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class GetSuccessorListResponse implements Message, Serializable {
    private List<NodeInterface> successorList;
    private Long lockId;

    public GetSuccessorListResponse(List<NodeInterface> successorList, Long lockId) {
        this.successorList = successorList;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public List<NodeInterface> getSuccessorList() {
        return successorList;
    }

    public Long getLockId() {
        return lockId;
    }
}
