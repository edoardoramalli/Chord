package controller.message;

import java.io.IOException;
import java.io.Serializable;

public interface NodeMessage extends Serializable {
    /**
     * Handles the message
     * @param nodeMessageHandler responsible of the handling
     * @throws IOException if an I/O error occurs
     */
    void handle(NodeMessageHandler nodeMessageHandler) throws IOException;
}
