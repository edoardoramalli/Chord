package controller.message;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface implemented by messages that go from Controller to Node
 */
public interface NodeMessage extends Serializable {
    /**
     * Handles the message
     * @param nodeMessageHandler responsible of the handling
     * @throws IOException if an I/O error occurs
     */
    void handle(NodeMessageHandler nodeMessageHandler) throws IOException;
}
