package controller.message;

import java.io.IOException;

public class StartLookupMessage implements ControllerMessage {
    private Long lockId;

    public StartLookupMessage(Long lockId) {
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
