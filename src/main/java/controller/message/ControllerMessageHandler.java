package controller.message;

import java.io.IOException;

public interface ControllerMessageHandler {
    void handle(ConnectedMessage connectedMessage) throws IOException;
}
