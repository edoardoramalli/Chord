package network.message;

import java.io.IOException;

public interface MessageHandler {

    void nodeDisconnected();

    void handle (FindSuccessorRequest findSuccessorRequest) throws IOException;

    void handle (FindSuccessorResponse findSuccessorResponse) throws IOException;

    void handle (NotifyRequest notifyRequest) throws IOException;

    void handle(TerminatedMethodMessage terminatedMethodMessage) throws IOException;

    void handle(CloseMessage closeMessage) throws IOException;

    void handle(GetPredecessorRequest getPredecessorRequest) throws IOException;

    void handle(GetPredecessorResponse getPredecessorResponse) throws IOException;

    void handle(GetDimFingerTableRequest getDimFingerTableRequest) throws IOException;

    void handle(GetDimFingerTableResponse getDimFingerTableResponse) throws IOException;

    void handle(GetIpAddressRequest getIpAddressRequest) throws IOException;

    void handle(GetIpAddressResponse getIpAddressResponse) throws IOException;

    void handle(GetSocketPortRequest getSocketPortRequest) throws IOException;

    void handle(GetSocketPortResponse getSocketPortResponse) throws IOException;

    void handle(GetSuccessorListRequest getSuccessorListRequest) throws IOException;

    void handle(GetSuccessorListResponse getSuccessorListResponse) throws IOException;
}
