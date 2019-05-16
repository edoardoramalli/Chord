package node;

import controller.SocketNodeStatistics;
import controller.NodeStatisticsController;
import exceptions.ConnectionErrorException;
import exceptions.NodeIdAlreadyExistsException;
import exceptions.TimerExpiredException;
import exceptions.UnexpectedBehaviourException;
import network.NodeCommunicator;
import network.SocketManager;
import network.SocketNodeListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static java.lang.System.*;

public class Node implements NodeInterface, Serializable {
    private static final long serialVersionUID = 1L;

    private String ipAddress;
    private int socketPort;
    private Long nodeId;

    private transient volatile CopyOnWriteArrayList<NodeInterface> successorList;
    private transient volatile NodeInterface predecessor;
    private transient volatile Map<Integer, NodeInterface> fingerTable;
    private transient int dimFingerTable;
    private transient int dimSuccessorList = 3;
    private transient int nextFinger;
    private transient volatile ConcurrentHashMap<Long, Object> keyStore;
    private transient volatile SocketManager socketManager;

    private transient volatile boolean stable = true;
    private transient volatile String ipController;
    private transient volatile int portController;
    private transient volatile NodeStatisticsController controller;

    public Node(String ipAddress, int socketPort) {
        this.ipAddress = ipAddress;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.socketPort = socketPort;
        this.nextFinger = 0;
        this.nodeId = -1L;
        this.socketManager = null;
        this.keyStore = new ConcurrentHashMap();
    }

    /**
     * Constructor used only in NodeCommunicator, when we send a Node through socket,
     * it takes also dimFingerTable as input in order to calculate the correspondent nodeId
     *
     * @param ipAddress      ipAddress of Node
     * @param socketPort     socketPort of Node
     * @param dimFingerTable dimension of finger table of the network
     */
    public Node(String ipAddress, int socketPort, int dimFingerTable) {
        this(ipAddress, socketPort);
        this.dimFingerTable = dimFingerTable;
        this.nodeId = Hash.getHash().calculateHash(ipAddress, socketPort);
    }

    public Node(String ipAddress, int socketPort, String ipController, int portController) {
        this(ipAddress, socketPort);
        this.ipController = ipController;
        this.portController = portController;
    }

    /**
     * This method is invoked only one time in the chord system. The node that does the creation of the network
     * specify its dimensions. Based on it, and its IPv4 address and port, the node compute its hash to get
     * the right nodeId. Start to listen on a specific port to accept incoming connection request to the network and
     * initialize the socketManger that manages all the sockets connection and the list of successor.
     * It also establish a socket to the controller, and shares with it its information.
     *
     * @param dimFingerTable is the power of two that will represent the dimension of the chord network
     * @throws ConnectionErrorException if the controller is not available
     * @throws IOException              if an I/O error occurs
     */
    public void create(int dimFingerTable) throws ConnectionErrorException, IOException {
        this.dimFingerTable = dimFingerTable;
        Hash.initializeHash(dimFingerTable);
        nodeId = Hash.getHash().calculateHash(ipAddress, socketPort);
        out.println("ID: " + nodeId);
        createSuccessorList();
        startSocketListener(socketPort);
        createFingerTable();
        socketManager = new SocketManager(this);
        controller = new SocketNodeStatistics(ipController, portController).openController(nodeId);
        controller.connected();
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    /**
     * If a node wants to enter into Chord Network, it has to join to a already present node. The node contacts the
     * node, receives all the information about the network. Then the node can calculate itself the right NodeId
     * and then does the same passages of the create procedure. In addition there is a control on the computed NodeId:
     * if it already present, another nodeId is computed. The node already presents in the network gives also to the
     * joining node its possible successor node.
     *
     * @param joinIpAddress  Ip address of a node already present in the Chord network
     * @param joinSocketPort Port of a node already present in the Chord network
     * @throws ConnectionErrorException     if the controller or the join node is not available.
     * @throws NodeIdAlreadyExistsException if the computed NodeId is already present in the network.
     * @throws IOException                  if an I/O error occurs
     */
    public void join(String joinIpAddress, int joinSocketPort)
            throws ConnectionErrorException, NodeIdAlreadyExistsException, IOException {
        startSocketListener(socketPort);
        NodeCommunicator nodeTemp = new NodeCommunicator(joinIpAddress, joinSocketPort,
                this, -1); //creates a temporary NodeCommunicator
        try {
            dimFingerTable = nodeTemp.getInitialDimFingerTable();
        } catch (TimerExpiredException e) {
            throw new ConnectionErrorException();
        }
        createFingerTable();
        Hash.initializeHash(dimFingerTable);
        this.nodeId = Hash.getHash().calculateHash(ipAddress, socketPort);
        out.println("ID: " + nodeId);
        createSuccessorList();
        this.socketManager = new SocketManager(this);
        NodeInterface successorNode;
        try {
            successorNode = nodeTemp.findSuccessor(this.nodeId);
        } catch (TimerExpiredException e) {
            throw new ConnectionErrorException();
        }
        if (successorNode.getNodeId().equals(nodeId)) //se find successor ritorna un nodo con lo stesso tuo id significa che esiste già un nodo con il tuo id
            throw new NodeIdAlreadyExistsException();
        nodeTemp.close();
        controller = new SocketNodeStatistics(ipController, portController).openController(nodeId);
        controller.connected();

        successorList.set(0, socketManager.createConnection(successorNode)); //creates a new connection
        try {
            initializeSuccessorList();
            successorList.get(0).notify(this); //serve per settare il predecessore nel successore del nodo
        } catch (TimerExpiredException e) {
            throw new ConnectionErrorException();
        }

        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    /**
     * Initializes fingerTable with this in all positions
     */
    private void createFingerTable() {
        for (int i = 0; i < dimFingerTable; i++)
            fingerTable.put(i, this);
    }

    /**
     * Creates successorList with this in the first position
     */
    private void createSuccessorList() {
        successorList = new CopyOnWriteArrayList<>();
        successorList.add(0, this);
    }

    /**
     * Asks to the successor its successorList, and constructs its own successorList from that
     *
     * @throws TimerExpiredException if getSuccessorList message do not has a response from the successor within a timer
     */
    private void initializeSuccessorList() throws TimerExpiredException {
        List<NodeInterface> successorNodeList = successorList.get(0).getSuccessorList();
        for (NodeInterface node : successorNodeList) {
            if (node.getNodeId().equals(successorList.get(0).getNodeId()) || node.getNodeId().equals(this.nodeId))
                break;
            if (successorList.size() < dimSuccessorList) {
                try {
                    successorList.add(socketManager.createConnection(node));
                } catch (ConnectionErrorException e) {
                    throw new UnexpectedBehaviourException();
                }
            }
        }
    }

    /**
     * The method, analyzing various cases, keep updated the successorList.
     *
     * @throws IOException           if an I/O error occurs
     * @throws TimerExpiredException if a timer expires
     */
    void listStabilize() throws IOException, TimerExpiredException {
        // The method first of all asks to the successor its own predecessor. If is it null, a notify starts on the
        // successor and the methods finish.
        NodeInterface x;
        x = successorList.get(0).getPredecessor();
        if (x == null) {
            successorList.get(0).notify(this);
            return;
        }
        // If the predecessor given form the successor is between the current node and its successor
        // it'll set it as its first successor and notify it.
        long nodeIndex = x.getNodeId();
        long oldSucID = successorList.get(0).getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID)) {
            try {
                socketManager.closeCommunicator(oldSucID);
                successorList.set(0, socketManager.createConnection(x));
            } catch (ConnectionErrorException e) {
                throw new UnexpectedBehaviourException();
            }
            successorList.get(0).notify(this);
        }

        // Now the node has to update its successor list. In order to do that it contacts its successor and
        // asks its successor list.

        boolean already = false;

        List<NodeInterface> xList; //xList contiene la lista dei successori del successore
        xList = successorList.get(0).getSuccessorList();
        if (successorList.size() < dimSuccessorList) { //Add new node to successor list
            for (NodeInterface xNode : xList) {
                if (!xNode.getNodeId().equals(nodeId) && successorList.size() < dimSuccessorList) {
                    try {
                        for (NodeInterface internalNode : successorList) {
                            if (internalNode.getNodeId().equals(xNode.getNodeId())) {
                                already = true;
                                break;
                            }
                        }
                        if (!already)
                            successorList.add(socketManager.createConnection(xNode));
                        already = false;
                    } catch (ConnectionErrorException e) {
                        throw new UnexpectedBehaviourException();
                    }
                } else {
                    break;
                }
            }
        } else { //only replace existing connection node in the successor list.
            int i;
            already = false;
            for (i = 1; i < dimSuccessorList && i < xList.size(); i++) {
                if (!successorList.get(i).getNodeId().equals(xList.get(i - 1).getNodeId())
                        && !xList.get(i - 1).getNodeId().equals(nodeId)) {
                    try {
                        for (int index = 0; index < i; index++) {
                            if (successorList.get(index).getNodeId().equals(xList.get(i - 1).getNodeId())) {
                                already = true;
                                break;
                            }
                            if (!already) {
                                socketManager.closeCommunicator(successorList.get(i).getNodeId());
                                successorList.set(i, socketManager.createConnection(xList.get(i - 1)));
                            }
                            already = false;
                        }
                    } catch (ConnectionErrorException e) {
                        throw new UnexpectedBehaviourException();
                    }
                }
            }
        }
        CopyOnWriteArrayList<NodeInterface> deleteList = (CopyOnWriteArrayList<NodeInterface>) successorList.clone();
        for (int z = 1; z < successorList.size(); z++) {
            if (successorList.get(z).equals(successorList.get(z - 1)))
                deleteList.remove(successorList.get(z));
        }
        successorList = deleteList;
    }

    /**
     * {@inheritDoc}
     *
     * @param id NodeId to be found
     * @return
     * @throws IOException
     * @throws TimerExpiredException
     */
    @Override
    public synchronized NodeInterface findSuccessor(Long id) throws IOException, TimerExpiredException {
        NodeInterface nextNode;
        for (NodeInterface nodeInterface : successorList) {
            if (checkIntervalEquivalence(nodeId, id, nodeInterface.getNodeId()))
                return nodeInterface;
        }
        nextNode = closestPrecedingNodeList(id);
        if (this == nextNode)
            return this;
        NodeInterface returnNode = nextNode.findSuccessor(id);
        if (returnNode == null)
            throw new TimerExpiredException();
        return returnNode;
    }

    /**
     * Find the closest preceding node starting to search in the successor list and then in the finger table.
     *
     * @param id find the closest preceding node of that id
     * @return The found node
     */
    private synchronized NodeInterface closestPrecedingNodeList(long id) {
        long nodeIndex;
        long maxClosestId = this.nodeId;
        NodeInterface maxClosestNode = this;

        //Check the node in the successor list starting from the last one. Save in maxClosestId
        // the temporary correct node.
        for (int i = successorList.size() - 1; i >= 0; i--) {
            nodeIndex = successorList.get(i).getNodeId();
            if (checkIntervalClosest(nodeIndex, id, this.nodeId)) {
                maxClosestId = nodeIndex;
                maxClosestNode = successorList.get(i);
                break;
            }
        }

        //After do the same thing as before with the finger table.
        for (int i = dimFingerTable - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkIntervalClosest(nodeIndex, id, this.nodeId))
                if (checkIntervalClosest(maxClosestId, nodeIndex, id))
                    return fingerTable.get(i);
                else
                    break;
        }
        return maxClosestNode;
    }

    /**
     * {@inheritDoc}
     *
     * @param node the node itself
     * @throws IOException
     */
    @Override
    public synchronized void notify(NodeInterface node) throws IOException {
        if (predecessor == null) {
            try {
                predecessor = socketManager.createConnection(node); //creo connessione
                moveKey();
            } catch (ConnectionErrorException e) {
                throw new UnexpectedBehaviourException();
            }
        } else {
            long index = node.getNodeId();
            long predIndex = predecessor.getNodeId();
            if (checkInterval(predIndex, index, getNodeId()) && !(predecessor.getNodeId().equals(node.getNodeId()))) { //entro solo se n è diverso dal predecessore
                try {
                    socketManager.closeCommunicator(predecessor.getNodeId());//chiudo connessione verso vecchio predecessore
                    predecessor = socketManager.createConnection(node); //apro connessione verso nuovo predecessore
                    moveKey();
                } catch (ConnectionErrorException e) {
                    throw new UnexpectedBehaviourException();
                }
            }
        }
    }

    /**
     * Method called by Main in order to send to controller the messages of start/end lookup
     *
     * @param id id of node to find
     * @return the found node
     * @throws IOException           if an I/O error occurs
     * @throws TimerExpiredException if lookup's timer expires
     */
    public NodeInterface startLookup(Long id) throws IOException, TimerExpiredException {
        controller.startLookup();
        NodeInterface searchedNode = lookup(id);
        controller.endOfLookup();
        return searchedNode;
    }

    /**
     * Receives an id to be found. Checks if the the node with that id is present inside in the successor list
     * or if it is the predecessor. Otherwise the search is forwarded to the findSuccessor method.
     *
     * @param id id of node to be found
     * @return The found node
     * @throws IOException           if an I/O error occurs
     * @throws TimerExpiredException if a timer expires
     */
    private synchronized NodeInterface lookup(Long id) throws IOException, TimerExpiredException {
        for (NodeInterface nodeInterface : successorList)
            if (id.equals(nodeInterface.getNodeId()))
                return nodeInterface;
        if (id.equals(predecessor.getNodeId()))
            return predecessor;
        else{
            NodeInterface returnNode = findSuccessor(id);
            if (returnNode == null)
                throw new TimerExpiredException();
            return returnNode;
        }
    }

    /**
     * The method has an internal state. It represents the line in the finger table. So to refresh the entire finger
     * table is necessary to call this method many times as finger table dimension.
     *
     * @throws IOException           if an I/O error occurs
     * @throws TimerExpiredException if a timer expires
     */
    void fixFingers() throws IOException, TimerExpiredException {
        long idToFind;
        nextFinger = nextFinger + 1;
        if (nextFinger > dimFingerTable)
            nextFinger = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, nextFinger - 1))) % (long) Math.pow(2, dimFingerTable);
        NodeInterface node = findSuccessor(idToFind);
        if (node == null)
            throw new TimerExpiredException();
        if (!node.getNodeId().equals(fingerTable.get(nextFinger - 1).getNodeId())) { //se il nuovo nodo è diverso da quello già presente
            NodeInterface newConnection;
            try {
                socketManager.closeCommunicator(fingerTable.get(nextFinger - 1).getNodeId());//chiudo connessione verso il vecchio nodo
                newConnection = socketManager.createConnection(node);
                fingerTable.replace(nextFinger - 1, newConnection);
            } catch (ConnectionErrorException e) {
                throw new UnexpectedBehaviourException();
            }
        }
        //else non faccio niente, perchè il vecchio nodo della finger è uguale a quello nuovo
    }

    /**
     * Check if the index is between pred and succ
     *
     * @param pred  previous nodeId
     * @param index nodeId of node to check
     * @param succ  successor nodeId
     * @return return true if index is between pred(excluded) and succ(excluded), otherwise return false
     * (return true if pred == succ)
     */
    private boolean checkInterval(long pred, long index, long succ) {
        if (pred == succ)
            return true;
        if (pred > succ)
            return (index > pred && index < Math.pow(2, dimFingerTable)) || (index >= 0 && index < succ);
        else
            return index > pred && index < succ;
    }

    /**
     * (Used in findSuccessor)
     *
     * @param pred  previous nodeId
     * @param index nodeId of node to check
     * @param succ  successor nodeId
     * @return return true if index is between pred(excluded) and succ(included), otherwise return false
     * (return true if pred == succ)
     */
    private boolean checkIntervalEquivalence(long pred, long index, long succ) {
        if (pred == succ)
            return true;
        if (pred > succ)
            return (index > pred && index < Math.pow(2, dimFingerTable)) || (index >= 0 && index <= succ);
        else
            return index > pred && index <= succ;
    }

    /**
     * (Used only in closestPrecedingNode to avoid infinite loops)
     *
     * @param pred  previous nodeId
     * @param index nodeId of node to check
     * @param succ  successor nodeId
     * @return return true if index is between pred(excluded) and succ(excluded), otherwise return false
     * (return false if pred == succ)
     */
    private boolean checkIntervalClosest(long pred, long index, long succ) {
        if (pred == succ)
            return false;
        if (pred > succ)
            return (index > pred && index < Math.pow(2, dimFingerTable)) || (index >= 0 && index < succ);
        else
            return index > pred && index < succ;
    }

    /**
     * Checks if the disconnected node (with nodeId = disconnectedId) is contained in some attributes,
     * and if it true substitute it with the default value (null for predecessor,
     * and this for successorList and fingerTable
     *
     * @param disconnectedId nodeId of disconnected node to check
     */
    public synchronized void checkDisconnectedNode(Long disconnectedId) {
        CopyOnWriteArrayList<NodeInterface> successorListClone = new CopyOnWriteArrayList<>(successorList);
        for (NodeInterface nodeInterface : successorListClone)
            if (nodeInterface.getNodeId().equals(disconnectedId)) //se il nodo disconnesso è il successore lo metto = this
                successorList.remove(nodeInterface);
        if (successorList.isEmpty())
            successorList.add(this);
        if (predecessor != null && predecessor.getNodeId().equals(disconnectedId)) //se il nodo disconnesso è il predecessore lo metto = null
            predecessor = null;
        for (int i = 0; i < dimFingerTable; i++) //se il nodo disconnesso è uno della finger lo metto = this
            if (fingerTable.get(i).getNodeId().equals(disconnectedId))
                fingerTable.replace(i, this);
    }

    /**
     * Called at the end of create and join, the method starts the thread responsible of accepting incoming connections
     *
     * @param socketPort port at which the other nodes have to connect
     */
    private void startSocketListener(int socketPort) {
        SocketNodeListener socketNodeListener = new SocketNodeListener(this, socketPort);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    /**
     * If the status of stability  is changed, a message is sent to the controller.
     *
     * @param stable At each iteration the node check if it is in a stable condition. If it changes its status,
     *               it'll send a message.
     */
    synchronized void updateStable(boolean stable) throws IOException {
        if (this.stable != stable) {
            this.stable = stable;
            if (!this.stable)
                controller.notStable();
            else
                controller.stable();
        }
    }

    /**
     * Method called by Main in order to send to controller the messages of start/end addKey
     *
     * @param keyValue the map element to be stored in the network
     * @return the node in which the key is stored
     * @throws IOException           if an I/O error occurs
     * @throws TimerExpiredException if addKey throws it
     */
    public NodeInterface startAddKey(Map.Entry<Long, Object> keyValue) throws IOException, TimerExpiredException {
        controller.startInsertKey();
        NodeInterface resultNode = addKey(keyValue);
        controller.endInsertKey();
        return resultNode;
    }

    /**
     * {@inheritDoc}
     *
     * @param keyValue the map element to be stored in the network
     * @return
     * @throws IOException
     * @throws TimerExpiredException
     */
    @Override
    public synchronized NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws IOException, TimerExpiredException {
        Long hashKey = keyValue.getKey() % (long) Math.pow(2, dimFingerTable);
        if (hashKey.equals(this.nodeId) || successorList.get(0).getNodeId().equals(this.getNodeId())) {
            addKeyToStore(keyValue);
            return this;
        }
        NodeInterface newNodeKey;
        if (predecessor != null && hashKey.equals(predecessor.getNodeId()))
            newNodeKey = predecessor;
        else
            newNodeKey = findSuccessor(hashKey);

        if (newNodeKey.getNodeId().equals(nodeId)) {
            addKeyToStore(keyValue);
            return this;
        }
        NodeInterface newNodeCommunicator;
        try {
            newNodeCommunicator = socketManager.createConnection(newNodeKey);
        } catch (ConnectionErrorException e) {
            throw new UnexpectedBehaviourException();
        }
        newNodeCommunicator.addKey(keyValue);
        socketManager.closeCommunicator(newNodeKey.getNodeId());
        return newNodeKey;
    }

    /**
     * {@inheritDoc}
     *
     * @param keyValue new key-value entry to be added
     */
    @Override
    public synchronized void addKeyToStore(Map.Entry<Long, Object> keyValue) {
        keyStore.put(keyValue.getKey(), keyValue.getValue());
    }

    /**
     * {@inheritDoc}
     *
     * @param key key to be retrieved from the set
     * @return {@inheritDoc}
     */
    @Override
    public synchronized Object retrieveKeyFromStore(Long key) {
        return keyStore.get(key);
    }

    /**
     * Moves some keys from a node to its new predecessor
     *
     * @throws IOException if an I/O error occurs
     */
    private synchronized void moveKey() throws IOException {
        for (Map.Entry<Long, Object> keyValue :
                keyStore.entrySet()) {
            long hashKey = keyValue.getKey() % (long) Math.pow(2, dimFingerTable);
            if (checkIntervalEquivalence(this.nodeId, hashKey, predecessor.getNodeId())) {
                try {
                    predecessor.addKey(new AbstractMap.SimpleEntry<>(keyValue.getKey(), keyValue.getValue()));
                    keyStore.remove(keyValue.getKey());
                } catch (TimerExpiredException ignore) {
                }
            }
        }
    }

    /**
     * Method called by Main in order to send to controller the messages of start/end findKey
     *
     * @param key owner of the key to be found
     * @return the found object if exist otherwise return null
     * @throws IOException           if an I/O error occurs
     * @throws TimerExpiredException if timer expires
     */
    public Object startFindKey(Long key) throws IOException, TimerExpiredException {
        controller.startFindKey();
        Object nodeWithKey = findKey(key);
        controller.endFindKey();
        return nodeWithKey;
    }

    /**
     * {@inheritDoc}
     *
     * @param key of the value that the node wants to find
     * @return {@inheritDoc}
     * @throws IOException
     * @throws TimerExpiredException
     */
    @Override
    public synchronized Object findKey(Long key) throws IOException, TimerExpiredException {
        long hashKey = key % (long) Math.pow(2, dimFingerTable);
        if (successorList.get(0).equals(this))
            return keyStore.get(key);

        if (predecessor != null && checkIntervalEquivalence(predecessor.getNodeId(), hashKey, nodeId))
            return keyStore.get(key);

        NodeInterface searchedNode = findSuccessor(hashKey);
        if (searchedNode == null)
            throw new TimerExpiredException();
        NodeInterface searchedNodeCommunicator;
        try {
            searchedNodeCommunicator = socketManager.createConnection(searchedNode);
        } catch (ConnectionErrorException e) {
            throw new UnexpectedBehaviourException();
        }
        Object searchedKey = searchedNodeCommunicator.findKey(key);
        socketManager.closeCommunicator(searchedNode.getNodeId());
        return searchedKey;
    }

    /**
     * This method handles the voluntarily departure of a node
     *
     * @throws IOException if an I/O error occurs
     */
    public synchronized void leave() throws IOException {
        transferKey();
        UpdateNode.stopUpdate();
        SocketNodeListener.stopListening();
        exit(0);
    }

    /**
     * This method transfers all the keys of a node to its successor
     *
     * @throws IOException if an I/O error occurs
     */
    private synchronized void transferKey() throws IOException {
        for (Map.Entry<Long, Object> keyValue :
                keyStore.entrySet()) {
            try {
                successorList.get(0).addKey(new AbstractMap.SimpleEntry<>(keyValue.getKey(), keyValue.getValue()));
            } catch (TimerExpiredException e) {
                err.println("Lost key. Successor is disconnected");
            }
            keyStore.remove(keyValue);
        }
    }

    @Override
    public Long getNodeId() {
        return nodeId;
    }

    /**
     * {@inheritDoc}
     * Not used in this class
     */
    @Override
    public void close() {
        throw new UnexpectedBehaviourException();
    }

    @Override
    public NodeInterface getPredecessor() {
        return predecessor;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Not used in this class
     *
     * @return {@inheritDoc}
     */
    @Override
    public int getInitialSocketPort() {
        throw new UnexpectedBehaviourException();
    }

    //Not used in this class
    @Override
    public void setSocketPort(int socketPort) {
        throw new UnexpectedBehaviourException();
    }

    @Override
    public int getSocketPort() {
        return socketPort;
    }

    Map<Integer, NodeInterface> getFingerTable() {
        return fingerTable;
    }

    /**
     * Not used in this class
     *
     * @return {@inheritDoc}
     */
    @Override
    public int getInitialDimFingerTable() {
        throw new UnexpectedBehaviourException();
    }

    @Override
    public int getDimFingerTable() {
        return dimFingerTable;
    }

    @Override
    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public SocketManager getSocketManager() {
        return socketManager;
    }

    @Override
    public List<NodeInterface> getSuccessorList() {
        return successorList;
    }

    @Override
    public String toString() {
        String string = "--------------------------\n" +
                "NODE ID: " + nodeId + "\n\n" +
                "PREDECESSOR:\t";
        if (predecessor != null)
            string = string + predecessor.getNodeId() + "\n";
        else
            string = string + "null\n";
        string = string +
                "SUCCESSOR LIST:";
        if (successorList != null)
            for (NodeInterface nodeInterface : successorList)
                string = string + "\t" + nodeInterface.getNodeId();
        string = string + "\n\n" +
                "FINGER TABLE:\n";
        for (int i = 0; i < fingerTable.size() && i < dimFingerTable; i++)
            string = string +
                    "\t\t" + fingerTable.get(i).getNodeId() + "\n";
        //KEY
        string = string + "MY KEY" + "\n";
        for (Map.Entry<Long, Object> keyValue :
                keyStore.entrySet()) {
            string = string + keyValue.getKey() + " " + keyValue.getValue() + "\n";
        }

        string = string + "--------------------------\n";
        return string;
    }
}