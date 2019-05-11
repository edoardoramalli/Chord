package controller.message;

import java.io.IOException;

public class ReceivedMessage implements ControllerMessage {
    private Long messageId;

    public ReceivedMessage(Long messageId) {
        this.messageId = messageId;
    }

    @Override
    public void handle(ControllerMessageHandler controllerMessageHandler) throws IOException {
        controllerMessageHandler.handle(this);
    }

    public Long getMessageId() {
        return messageId;
    }
}
