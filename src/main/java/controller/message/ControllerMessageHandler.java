package controller.message;

import java.io.IOException;

/**
 * Interface implemented by ControllerCommunicator, responsible of handling the messages from Node
 */
public interface ControllerMessageHandler {
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

    /**
     * @param startInsertKeyMessage the received startInsertKeyMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(StartInsertKeyMessage startInsertKeyMessage) throws IOException;

    /**
     * @param endInsertKeyMessage the received endInsertKeyMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(EndInsertKeyMessage endInsertKeyMessage) throws IOException;

    /**
     * @param startFindKeyMessage the received startFindKeyMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(StartFindKeyMessage startFindKeyMessage) throws IOException;

    /**
     * @param endFindKeyMessage the received endFindKeyMessage
     * @throws IOException if an I/O error occurs
     */
    void handle(EndFindKeyMessage endFindKeyMessage) throws IOException;
}