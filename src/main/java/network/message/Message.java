package network.message;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface implemented by node's messages
 */
public interface Message extends Serializable {

    /**
     * Handles the message
     * @param messageHandler responsible of the handling
     * @throws IOException in an I/O error occurs
     */
    void handle(MessageHandler messageHandler) throws IOException;
}
