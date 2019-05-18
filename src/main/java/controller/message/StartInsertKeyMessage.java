package controller.message;

import java.io.IOException;

public class StartInsertKeyMessage implements ControllerMessage {
    private Long lockId;

    public StartInsertKeyMessage(Long lockId) {
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
