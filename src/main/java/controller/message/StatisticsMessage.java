package controller.message;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface implemented by messages that go from Node to Statistics
 */
public interface StatisticsMessage extends Serializable {

    /**
     * Handles the message
     * @param statisticsMessageHandler responsible of the handling
     * @throws IOException if an I/O error occurs
     */
    void handle(StatisticsMessageHandler statisticsMessageHandler) throws IOException;
}
