package controller;

import java.io.IOException;

/**
 * Interface implemented by NodeControllerCommunicator and DisconnectedController
 */
public interface ControllerInterface {
    void connected() throws IOException;

    void stable() throws IOException;

    void notStable() throws IOException;

    void startLookup() throws IOException;

    void endOfLookup() throws IOException;

    void startInsertKey() throws IOException;

    void endInsertKey() throws IOException;

    void startFindKey() throws IOException;

    void endFindKey() throws IOException;
}
