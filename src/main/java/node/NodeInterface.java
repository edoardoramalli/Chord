package node;

import network.SocketManager;

import java.io.IOException;
import java.io.Serializable;

public interface NodeInterface extends Serializable {

    void notify(NodeInterface node) throws IOException;

    void checkPredecessor();

    NodeInterface findSuccessor(Long id) throws IOException;

    NodeInterface getPredecessor() throws IOException;

    String getIpAddress() throws IOException;

    int getSocketPort() throws IOException;

    int getDimFingerTable() throws IOException;

    Long getNodeId();

    void close() throws IOException;

    SocketManager getSocketManager();

}
