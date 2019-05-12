package controller.message;

import java.io.IOException;

/**
 * Interface implemented by NodeStatisticsController, responsible of handling the messages from Statistics
 */
public interface NodeMessageHandler {
    /**
     * @param receivedMessage the received ReceivedMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(ReceivedMessage receivedMessage) throws IOException;
}