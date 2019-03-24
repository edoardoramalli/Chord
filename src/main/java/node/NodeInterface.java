package node;

import exceptions.ConnectionErrorException;
import network.SocketNode;

import java.io.IOException;
import java.io.Serializable;

public interface NodeInterface extends Serializable {

    NodeInterface createConnection(SocketNode socketNode) throws IOException;

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

}
