package controller.message;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface implemented by messages that go from Node to Controller
 */
public interface ControllerMessage extends Serializable {

    /**
     * Handles the message
     *
     * @param controllerMessageHandler responsible of the handling
     * @throws IOException if an I/O error occurs
     */
    void handle(ControllerMessageHandler controllerMessageHandler) throws IOException;
}
