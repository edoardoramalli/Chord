package controller.message;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface implemented by controller's messages
 */
public interface ControllerMessage extends Serializable {

    /**
     * Handles the message
     * @param controllerMessageHandler responsible of the handling
     * @throws IOException if an I/O error occurs
     */
    void handle(ControllerMessageHandler controllerMessageHandler) throws IOException;
}
