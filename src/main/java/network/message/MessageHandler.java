package network.message;

import java.io.IOException;

public interface MessageHandler {

    void handle (FindSuccessorRequest findSuccessorRequest) throws IOException;

    void handle (FindSuccessorResponse findSuccessorResponse) throws IOException;
}
