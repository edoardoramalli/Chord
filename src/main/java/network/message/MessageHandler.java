package network.message;

import java.io.IOException;

public interface MessageHandler {

    void handle (FindSuccessorRequest findSuccessorRequest) throws IOException;

    void handle (FindSuccessorResponse findSuccessorResponse) throws IOException;

    void handle (NotifyRequest notifyRequest) throws IOException;

    void handle(TerminatedMethodMessage terminatedMethodMessage) throws IOException;

    void handle(CloseMessage closeMessage) throws IOException;
}
