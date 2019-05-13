package controller.message;

import java.io.IOException;

public class ReceivedMessage implements NodeMessage {
    private Long lockId;

    public ReceivedMessage(Long lockId) {
        this.lockId = lockId;
    }

    @Override
    public void handle(NodeMessageHandler nodeMessageHandler) throws IOException {
        nodeMessageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }
}