package controller.message;

import java.io.IOException;

public class EndInsertKeyMessage implements ControllerMessage {
    private Long lockId;

    public EndInsertKeyMessage(Long lockId) {
        this.lockId = lockId;
    }

    @Override
    public void handle(ControllerMessageHandler controllerMessageHandler) throws IOException {
        controllerMessageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }
}