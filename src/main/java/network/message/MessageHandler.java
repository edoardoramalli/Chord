package network.message;

import java.io.IOException;

/**
 * Interface implemented by NodeCommunicator that is responsible of handling the messages from other nodes
 */
public interface MessageHandler {

    /**
     * Method called by socketNode when the node to which the connection is open is disconnected.
     * Method calls SocketManager.removeNode(disconnectedNodeId)
     */
    void nodeDisconnected();

    /**
     * @param findSuccessorRequest the received findSuccessorRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle (FindSuccessorRequest findSuccessorRequest) throws IOException;

    /**
     * @param findSuccessorResponse the received findSuccessorResponse message
     * @throws IOException in an I/O error occurs
     */
    void handle (FindSuccessorResponse findSuccessorResponse) throws IOException;

    /**
     * @param notifyRequest the received notifyRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle (NotifyRequest notifyRequest) throws IOException;

    /**
     * @param terminatedMethodMessage the received TerminatedMethodMessage message
     * @throws IOException in an I/O error occurs
     */
    void handle(TerminatedMethodMessage terminatedMethodMessage) throws IOException;

    /**
     * @param closeRequest the received closeRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(CloseRequest closeRequest) throws IOException;

    /**
     * @param getPredecessorRequest the received getPredecessorRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetPredecessorRequest getPredecessorRequest) throws IOException;

    /**
     * @param getPredecessorResponse the received getPredecessorResponse message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetPredecessorResponse getPredecessorResponse) throws IOException;

    /**
     * @param getDimFingerTableRequest the received getDimFingerTableRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetDimFingerTableRequest getDimFingerTableRequest) throws IOException;

    /**
     * @param getDimFingerTableResponse the received getDimFingerTableResponse message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetDimFingerTableResponse getDimFingerTableResponse) throws IOException;

    /**
     * @param getInitialSocketPortRequest the received getInitialSocketPortRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetInitialSocketPortRequest getInitialSocketPortRequest) throws IOException;

    /**
     * @param getInitialSocketPortResponse the received getInitialSocketPortResponse message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetInitialSocketPortResponse getInitialSocketPortResponse) throws IOException;

    /**
     * @param getSuccessorListRequest the received getSuccessorListRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetSuccessorListRequest getSuccessorListRequest) throws IOException;

    /**
     * @param getSuccessorListResponse the received getSuccessorListResponse message
     * @throws IOException in an I/O error occurs
     */
    void handle(GetSuccessorListResponse getSuccessorListResponse) throws IOException;

    /**
     * @param addKeyRequest the received addKeyRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(AddKeyRequest addKeyRequest) throws IOException;

    /**
     * @param addKeyResponse the received addKeyResponse message
     * @throws IOException in an I/O error occurs
     */
    void handle(AddKeyResponse addKeyResponse) throws IOException;

    /**
     * @param findKeyResponse the received findKeyResponse message
     * @throws IOException in an I/O error occurs
     */
    void handle(FindKeyResponse findKeyResponse) throws IOException;

    /**
     * @param findKeyRequest the received findKeyRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(FindKeyRequest findKeyRequest) throws IOException;

    /**
     * @param leaveRequest the received leaveRequest message
     * @throws IOException in an I/O error occurs
     */
    void handle(LeaveRequest leaveRequest) throws IOException;
}
