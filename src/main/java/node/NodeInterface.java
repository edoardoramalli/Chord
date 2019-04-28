package node;

import exceptions.TimerExpiredException;
import network.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface NodeInterface extends Serializable {

    SocketManager getSocketManager();

    void notify(NodeInterface node) throws IOException, TimerExpiredException;

    NodeInterface findSuccessor(Long id) throws IOException, TimerExpiredException;

    NodeInterface getPredecessor() throws IOException, TimerExpiredException;

    String getIpAddress();

    int getInitialSocketPort() throws IOException, TimerExpiredException;

    void setSocketPort(int socketPort);

    int getSocketPort();

    int getDimFingerTable() throws IOException, TimerExpiredException;

    void setNodeId(Long nodeId);

    Long getNodeId();

    void close() throws IOException;

    List<NodeInterface> getSuccessorList() throws IOException, TimerExpiredException;

    //Controller

    void sendToController (String text);

    //Key

    public NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws IOException;

    void addKeyToStore(Map.Entry<Long, Object> keyValue);

    Object findKey(Long key) throws IOException;

    Object retrieveKeyFromStore(Long key);
}
