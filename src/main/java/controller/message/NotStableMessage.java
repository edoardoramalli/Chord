package controller.message;

import java.io.IOException;

public class NotStableMessage implements ControllerMessage {
    private Long lockId;

    public NotStableMessage(Long lockId) {
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