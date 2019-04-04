package node;

import network.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public interface NodeInterface extends Serializable {

    SocketManager getSocketManager();

    void notify(NodeInterface node) throws IOException;

    void checkPredecessor();

    NodeInterface findSuccessor(Long id) throws IOException;

    void sendTextMessage(Long source, Long dest, String textMessage) throws IOException;

    NodeInterface getPredecessor() throws IOException;

    String getIpAddress() throws IOException;

    int getSocketPort() throws IOException;

    int getDimFingerTable() throws IOException;

    Long getNodeId();

    void close() throws IOException;

    List<NodeInterface> getSuccessorList() throws IOException;

}
