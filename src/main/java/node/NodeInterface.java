package node;

import exceptions.ConnectionErrorException;
import network.SocketManager;
import network.SocketNode;

import java.io.IOException;
import java.io.Serializable;

public interface NodeInterface extends Serializable {

    void notify(NodeInterface node) throws IOException;

    void checkPredecessor();

    NodeInterface findSuccessor(Long id) throws IOException;

    NodeInterface getPredecessor() throws IOException;

    NodeInterface getSuccessor();

    String getIpAddress() throws IOException;

    int getSocketPort() throws IOException;

    int getDimFingerTable() throws IOException;

    Long getNodeId() throws IOException;

    void close() throws IOException;

    Long getHostId();

    SocketManager getSocketManager();

}
