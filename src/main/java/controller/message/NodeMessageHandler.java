package controller.message;

import java.io.IOException;

public interface NodeMessageHandler {
    void handle(ReceivedMessage receivedMessage) throws IOException;
}