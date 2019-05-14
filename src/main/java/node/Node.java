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

import static java.lang.System.exit;
import static java.lang.System.out;

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
     * @param ipAddress ipAddress of Node
     * @param socketPort socketPort of Node
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
        createSuccessorList(); //TODO questo secondo me si può togliere tanto dopo fa la initialize
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

    synchronized void listStabilize() throws IOException, TimerExpiredException {
        //questo serve per settare il primo successore
        // out.println("---------->");
        NodeInterface x;
        x = successorList.get(0).getPredecessor();
        if (x == null) {
            successorList.get(0).notify(this);
            return;
        }
        long nodeIndex = x.getNodeId();
        long oldSucID = successorList.get(0).getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID)) {
            try {

                socketManager.closeCommunicator(oldSucID);
                successorList.set(0, socketManager.createConnection(x));
            } catch (ConnectionErrorException e) {
                throw new UnexpectedBehaviourException();
            }
        }
        successorList.get(0).notify(this);

        boolean already = false;

        List<NodeInterface> xList; //xList contiene la lista dei successori del successore
        xList = successorList.get(0).getSuccessorList();
        if (successorList.size() < dimSuccessorList) {
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
                }
            }
        } else {
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
    //catch successor exception--->update listSuccessor

    /**
     * {@inheritDoc}
     * @param id NodeId to be found
     * @return
     * @throws IOException
     * @throws TimerExpiredException
     */
    @Override
    public NodeInterface findSuccessor(Long id) throws IOException, TimerExpiredException {
        NodeInterface nextNode;
        for (NodeInterface nodeInterface : successorList) {
            if (checkIntervalEquivalence(nodeId, id, nodeInterface.getNodeId()))
                return nodeInterface;
        }
        nextNode = closestPrecedingNodeList(id);
        if (this == nextNode)
            return this;
        return nextNode.findSuccessor(id);
    }

    private NodeInterface closestPrecedingNodeList(long id) {
        long nodeIndex;
        long maxClosestId = this.nodeId;
        NodeInterface maxClosestNode = this;

        for (int i = successorList.size() - 1; i >= 0; i--) {
            nodeIndex = successorList.get(i).getNodeId();
            if (checkIntervalClosest(nodeIndex, id, this.nodeId)) {
                maxClosestId = nodeIndex;
                maxClosestNode = successorList.get(i);
                break;
            }
        }

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
     * @param id id of node to find
     * @return the found node
     * @throws IOException if an I/O error occurs
     * @throws TimerExpiredException if lookup's timer expires
     */
    public NodeInterface startLookup(Long id) throws IOException, TimerExpiredException {
        controller.startLookup();
        NodeInterface searchedNode = lookup(id);
        controller.endOfLookup();
        return searchedNode;
    }

    private NodeInterface lookup(Long id) throws IOException, TimerExpiredException {
        for (NodeInterface nodeInterface : successorList)
            if (id.equals(nodeInterface.getNodeId()))
                return nodeInterface;
        if (id.equals(predecessor.getNodeId()))
            return predecessor;
        else
            return findSuccessor(id);
    }

    synchronized void fixFingers() throws IOException, TimerExpiredException {
        long idToFind;
        nextFinger = nextFinger + 1;
        if (nextFinger > dimFingerTable)
            nextFinger = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, nextFinger - 1))) % (long) Math.pow(2, dimFingerTable);
        NodeInterface node = findSuccessor(idToFind);
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
     * @param disconnectedId nodeId of disconnected node to check
     */
    public void checkDisconnectedNode(Long disconnectedId) {
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
     * @param socketPort port at which the other nodes have to connect
     */
    private void startSocketListener(int socketPort) {
        SocketNodeListener socketNodeListener = new SocketNodeListener(this, socketPort);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    /**
     * @param stable At each iteration the node check if it is in a stable condition. If it changes its status,
     *               it'll send a message.
     */
    void updateStable(boolean stable) throws IOException {
        if (this.stable != stable) {
            this.stable = stable;
            if (!this.stable) {
                controller.notStable();
            } else {
                controller.stable();
            }
        }
    }

    /**
     * Method called by Main in order to send to controller the messages of start/end addKey
     * @param keyValue
     * @return
     * @throws IOException if an I/O error occurs
     * @throws TimerExpiredException if addKey throws it
     */
    public NodeInterface startAddKey(Map.Entry<Long, Object> keyValue) throws IOException, TimerExpiredException {
        controller.startInsertKey();
        NodeInterface resultNode = addKey(keyValue);
        controller.endInsertKey();
        return resultNode;
    }

    /**
     * //todo da rigenerare dopo aver fatto in nodeInterface
     * {@inheritDoc}
     * @param keyValue
     * @return
     * @throws IOException
     * @throws TimerExpiredException
     */
    @Override
    public NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws IOException, TimerExpiredException {
        Long hashKey = keyValue.getKey() % (long) Math.pow(2, dimFingerTable);
        if (hashKey.equals(this.nodeId) || successorList.get(0).equals(this)) {
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
        newNodeKey.addKey(keyValue);
        return newNodeKey;
    }

    /**
     * {@inheritDoc}
     * @param keyValue new key-value entry to be added
     */
    @Override
    public void addKeyToStore(Map.Entry<Long, Object> keyValue) {
        keyStore.put(keyValue.getKey(), keyValue.getValue());
    }

    /**
     * {@inheritDoc}
     * @param key key to be retrieved from the set
     * @return {@inheritDoc}
     */
    @Override
    public Object retrieveKeyFromStore(Long key) {
        return keyStore.get(key);
    }

    /**
     * Moves some keys from a node to its new predecessor
     * @throws IOException if an I/O error occurs
     */
    private void moveKey() throws IOException {
        for (Map.Entry<Long, Object> keyValue :
                keyStore.entrySet()) {
            long hashKey = keyValue.getKey() % (long) Math.pow(2, dimFingerTable);
            if (checkIntervalEquivalence(this.nodeId, hashKey, predecessor.getNodeId())) {
                try {
                    predecessor.addKey(new AbstractMap.SimpleEntry<>(keyValue.getKey(), keyValue.getValue()));
                } catch (TimerExpiredException e) {//TODO gestire eccezione
                    e.printStackTrace();
                }
                keyStore.remove(keyValue.getKey());
            }
        }
    }

    /**
     * Method called by Main in order to send to controller the messages of start/end findKey
     * @param key
     * @return
     * @throws IOException
     * @throws TimerExpiredException
     */
    public Object startFindKey(Long key) throws IOException, TimerExpiredException {
        controller.startFindKey();
        Object nodeWithKey = findKey(key);
        controller.endFindKey();
        return nodeWithKey;
    }

    /**
     * {@inheritDoc}
     * @param key of the value that the node wants to find
     * @return {@inheritDoc}
     * @throws IOException
     * @throws TimerExpiredException
     */
    @Override
    public Object findKey(Long key) throws IOException, TimerExpiredException {
        long hashKey = key % (long) Math.pow(2, dimFingerTable);
        if (successorList.get(0).equals(this))
            return keyStore.get(key);

        if (predecessor != null && checkIntervalEquivalence(predecessor.getNodeId(), hashKey, nodeId))
            return keyStore.get(key);

        return findSuccessor(hashKey).findKey(key);
    }

    /**
     * This method handles the voluntarily departure of a node
     * @throws IOException if an I/O error occurs
     */
    public void leave() throws IOException {
        transferKey();
        UpdateNode.stopUpdate();
        SocketNodeListener.stopListening();
        exit(0);
    }

    /**
     * This method transfers all the keys of a node to its successor
     * @throws IOException if an I/O error occurs
     */
    private void transferKey() throws IOException {
        for (Map.Entry<Long, Object> keyValue :
                keyStore.entrySet()) {
            try {
                successorList.get(0).addKey(new AbstractMap.SimpleEntry<>(keyValue.getKey(), keyValue.getValue()));
            } catch (TimerExpiredException e) {//TODO gestire eccezione
                e.printStackTrace();
            }
            keyStore.remove(keyValue);
        }
    }

    /**
     * {@inheritDoc}
     * @param oldNodeID the node that left the network
     * @param newNode   the node that replaces the one that left
     * @throws ConnectionErrorException
     */
    @Override
    public synchronized void updateAfterLeave(Long oldNodeID, NodeInterface newNode)
            throws ConnectionErrorException {
        if (oldNodeID.equals(predecessor.getNodeId())) {
            out.println("My predecessor left");
            if (successorList.contains(predecessor)) {
                successorList.remove(predecessor);
                socketManager.closeCommunicator(oldNodeID);
            }

            predecessor = socketManager.createConnection(newNode);
            socketManager.closeCommunicator(oldNodeID);
            out.println(this.toString());
        }

        if (oldNodeID.equals(successorList.get(0).getNodeId())) {
            out.println("My successor left");
            successorList.remove(0);
            NodeInterface newCommunicator = socketManager.createConnection(newNode);
            if (!successorList.contains(newCommunicator) && !newCommunicator.getNodeId().equals(nodeId))
                successorList.add(newCommunicator);
            socketManager.closeCommunicator(oldNodeID);

            out.println(this.toString());
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