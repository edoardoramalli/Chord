package controller.message;

import java.io.IOException;

public class ConnectedMessage implements ControllerMessage {
    private Long nodeId;
    private Long lockId;

    public ConnectedMessage(Long nodeId, Long lockId) {
        this.nodeId = nodeId;
        this.lockId = lockId;
    }

    @Override
    public void handle(ControllerMessageHandler controllerMessageHandler) throws IOException {
        controllerMessageHandler.handle(this);
    }

    public Long getNodeId() {
        return nodeId;
    }

    public Long getLockId() {
        return lockId;
    }
}