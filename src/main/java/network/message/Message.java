package network.message;

import java.io.IOException;

public interface Message {

    void handle(MessageHandler messageHandler) throws IOException;
}
