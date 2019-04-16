package node;

import network.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface NodeInterface extends Serializable {

    SocketManager getSocketManager();

    void notify(NodeInterface node) throws IOException;

    NodeInterface findSuccessor(Long id) throws IOException;

    NodeInterface getPredecessor() throws IOException;

    String getIpAddress() throws IOException;

    int getSocketPort() throws IOException;

    int getDimFingerTable() throws IOException;

    void setNodeId(Long nodeId);

    Long getNodeId();

    void close() throws IOException;

    List<NodeInterface> getSuccessorList() throws IOException;

    //Key

    public NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws IOException;

    void sendToController (String text);

    void addKeyToStore(Map.Entry<Long, Object> keyValue);

    Object findKey(Long key) throws IOException;

    Object retrieveKeyFromStore(Long key);
}
