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

    /**
     * Notify method is called on a node passing as parameter the caller node. In this way the caller tells
     * to the callee that it maybe be its predecessor. The caller checks if this is true. The check done by
     * the callee is : if its predecessor is not yet set, the node will set the parameter node
     * (in input to the function) as predecessor. Otherwise, if a predecessor is already set,
     * thanks to the nodeId of the node parameter, checks if it is between its actual predecessor and the nodeId itself.
     * If the check is true the parameter node is set as predecessor, otherwise nothing is happened.
     * @param node the node itself
     * @throws IOException if an I/O error occurs
     * @throws TimerExpiredException if timer expires
     */
    void notify(NodeInterface node) throws IOException, TimerExpiredException;

    /**
     * The method finds the responsible node respect to the id received as parameter.
     * Initially checks if the NodeId is present in the successorList. If is true return the object related to the
     * requested nodeId. Then, if is not present, call the 'closestPrecedingNodeList' method, given NodeId as parameter.
     * At the end the the method is called on the node returned form the 'closestPrecedingNodeList'. If the node
     * is itself return the current node.
     * @param id NodeId to be found
     * @return The found Node object
     * @throws IOException if an I/O error occurs
     * @throws TimerExpiredException if timer expires
     */
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
     * Add to the couple < owner_id, object> to the network
     *
     * @param keyValue the map element to be stored in the network
     * @return the node in which the key is stored
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
}
