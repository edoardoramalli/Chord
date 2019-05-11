package controller.message;

import java.io.IOException;

public interface ControllerMessageHandler {
    void handle(ConnectedMessage connectedMessage) throws IOException;

    void handle(StableMessage stableMessage) throws IOException;

    void handle(NotStableMessage notstableMessage) throws IOException;
}