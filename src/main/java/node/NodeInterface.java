package node;

import exceptions.ConnectionErrorException;
import exceptions.TimerExpiredException;
import network.SocketManager;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface NodeInterface extends Serializable {

    /**
     * @return the socketManager
     */
    SocketManager getSocketManager();

    void notify(NodeInterface node) throws IOException, TimerExpiredException;

    NodeInterface findSuccessor(Long id) throws IOException, TimerExpiredException;

    NodeInterface getPredecessor() throws IOException, TimerExpiredException;

    String getIpAddress();

    /**
     * Used to ask the node its socketPort for the first time, after the port is saved locally and will be
     * accessed through getSocketPort()
     * @return socketPort of other node
     * @throws TimerExpiredException if timer expires
     */
    int getInitialSocketPort() throws TimerExpiredException;

    void setSocketPort(int socketPort);

    int getSocketPort();

    /**
     * Used to ask the node the dimension of the finger table for the first time,
     * after the port is saved locally and will be accessed through getDimFingerTable()
     * (Used only during join phase)
     * @return dimension of finger table
     * @throws TimerExpiredException if timer expires
     */
    int getInitialDimFingerTable() throws TimerExpiredException;

    int getDimFingerTable();

    void setNodeId(Long nodeId);

    Long getNodeId();

    void close() throws IOException;

    List<NodeInterface> getSuccessorList() throws TimerExpiredException;

    //Key

    /**
     * //TODO da fare
     * @param keyValue
     * @return
     * @throws IOException if an I/O error occurs
     * @throws TimerExpiredException if timer expires
     */
    NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws IOException, TimerExpiredException;

    /**
     * This method handles the search of a value given its key
     * @param key of the value that the node wants to find
     * @return the value if it exists, null otherwise
     * @throws IOException if an I/O error occurs
     * @throws TimerExpiredException if timer expires
     */
    Object findKey(Long key) throws IOException, TimerExpiredException;

    /**
     * This method adds the new key-value tuple to the local set of the keys that the node has
     * @param keyValue new key-value entry to be added
     */
    void addKeyToStore(Map.Entry<Long, Object> keyValue);

    /**
     * Retrieves a value given a key from the local set of a key
     * @param key key to be retrieved from the set
     * @return the value found, null otherwise
     */
    Object retrieveKeyFromStore(Long key);

    /**
     * This method handles the update after that a node voluntarily left the network
     * @param oldNodeID the node that left the network
     * @param newNode the node that replaces the one that left
     * @throws IOException if an I/O error occurs
     * @throws ConnectionErrorException if node is not reachable
     */
    void updateAfterLeave(Long oldNodeID, NodeInterface newNode) throws IOException, ConnectionErrorException;
}
