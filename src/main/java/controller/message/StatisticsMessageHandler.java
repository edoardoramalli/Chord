package controller.message;

import java.io.IOException;

public interface StatisticsMessageHandler {
    /**
     * @param connectedMessage the received connectedMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(ConnectedMessage connectedMessage) throws IOException;

    /**
     * @param stableMessage the received stableMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(StableMessage stableMessage) throws IOException;

    /**
     * @param notStableMessage the received notStableMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(NotStableMessage notStableMessage) throws IOException;

    /**
     * @param startLookupMessage the received startLookupMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(StartLookupMessage startLookupMessage) throws IOException;

    /**
     * @param endOfLookupMessage the received endOfLookupMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(EndOfLookupMessage endOfLookupMessage) throws IOException;
}