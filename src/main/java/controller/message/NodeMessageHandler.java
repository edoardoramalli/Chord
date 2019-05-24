package controller.message;

import java.io.IOException;

/**
 * Interface implemented by NodeControllerCommunicator, responsible of handling the messages from Controller
 */
public interface NodeMessageHandler {
    /**
     * @param receivedMessage the received ReceivedMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(ReceivedMessage receivedMessage) throws IOException;
}