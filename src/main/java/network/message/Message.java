package network.message;

import java.io.IOException;
import java.io.Serializable;

public interface Message extends Serializable {

    void handle(MessageHandler messageHandler) throws IOException;
}
