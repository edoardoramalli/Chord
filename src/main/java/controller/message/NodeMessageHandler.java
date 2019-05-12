package controller.message;

import java.io.IOException;

public interface NodeMessageHandler {
    /**
     * @param receivedMessage the received ReceivedMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(ReceivedMessage receivedMessage) throws IOException;
}