package node;

import exceptions.ConnectionErrorException;
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

    /**
     * Used to ask the node its socketPort for the first time, after the port is saved locally and will be
     * accessed through getSocketPort(
     * @return socketPort of other node
     * @throws TimerExpiredException if timer expires
     */
    int getInitialSocketPort() throws TimerExpiredException;

    void setSocketPort(int socketPort);

    int getSocketPort();

    int getInitialDimFingerTable() throws IOException, TimerExpiredException;

    int getDimFingerTable();

    void setNodeId(Long nodeId);

    Long getNodeId();

    void close() throws IOException;

    List<NodeInterface> getSuccessorList() throws IOException, TimerExpiredException;

    //Controller

    void sendToController (String text);

    //Key

    NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws IOException, TimerExpiredException;

    void addKeyToStore(Map.Entry<Long, Object> keyValue);

    Object findKey(Long key) throws IOException, TimerExpiredException;

    Object retrieveKeyFromStore(Long key);

    void updateAfterLeave(Long oldNodeID, NodeInterface newNode) throws IOException, ConnectionErrorException;
}
