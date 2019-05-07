package node;

import exceptions.ConnectionErrorException;
import exceptions.NodeIdAlreadyExistsException;
import exceptions.TimerExpiredException;
import exceptions.UnexpectedBehaviourException;
import network.NodeCommunicator;
import network.SocketManager;
import network.SocketNodeListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

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
    //connection handler
    private transient volatile SocketManager socketManager;

    private transient volatile PrintWriter outBuffer;
    private transient volatile Socket socketController;
    private transient volatile boolean stable = true;
    private transient volatile String ipController;
    private transient volatile int portController;

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

    public Node(String ipAddress, int socketPort, int dimFingerTable) {
        this(ipAddress, socketPort);
        this.dimFingerTable = dimFingerTable;
        this.nodeId = hash(ipAddress, socketPort);

    }

    public Node(String ipAddress, int socketPort, String ipController, int portController) {
        this(ipAddress, socketPort);
        this.ipController = ipController;
        this.portController = portController;
    }

    public void create(int m) {
        dimFingerTable = m;
        nodeId = hash(ipAddress, socketPort);
        out.println("ID: " + nodeId);
        createSuccessorList();
        startSocketListener(socketPort);
        createFingerTable();
        socketManager = new SocketManager(this);
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
        openController();
    }

    public void join(String joinIpAddress, int joinSocketPort)
            throws ConnectionErrorException, NodeIdAlreadyExistsException, IOException {
        startSocketListener(socketPort);

        NodeCommunicator nodeTemp = new NodeCommunicator(joinIpAddress, joinSocketPort,
                this, hash(joinIpAddress, joinSocketPort)); // crea un nodecomunicator temporaneo.
        try {
            dimFingerTable = nodeTemp.getInitialDimFingerTable();
        } catch (TimerExpiredException e) {
            throw new ConnectionErrorException();
        }
        createFingerTable();
        this.nodeId = hash(ipAddress, socketPort);
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
        openController();

        successorList.set(0, socketManager.createConnection(successorNode)); //creo nuova connessione
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
    //The successor list is initialized with only this
    private void createSuccessorList() {
        successorList = new CopyOnWriteArrayList<>();
        successorList.add(0, this);
    }

    /**
     * Asks to the successor its successorList, and constructs its own successorList from that
     * @throws IOException if an I/O error occurs
     * @throws TimerExpiredException if getSuccessorList message do not has a response from the successor within a timer
     */
    private void initializeSuccessorList() throws IOException, TimerExpiredException {
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

    @Override
    public synchronized void notify(NodeInterface n) throws IOException {
        if (predecessor == null) {
            try {
                predecessor = socketManager.createConnection(n); //creo connessione
                moveKey();
            } catch (ConnectionErrorException e) {
                throw new UnexpectedBehaviourException();
            }
        } else {
            long index = n.getNodeId();
            long predIndex = predecessor.getNodeId();
            if (checkInterval(predIndex, index, getNodeId()) && !(predecessor.getNodeId().equals(n.getNodeId()))) { //entro solo se n è diverso dal predecessore
                try {

                    socketManager.closeCommunicator(predecessor.getNodeId());//chiudo connessione verso vecchio predecessore
                    predecessor = socketManager.createConnection(n); //apro connessione verso nuovo predecessore
                    moveKey();
                } catch (ConnectionErrorException e) {
                    throw new UnexpectedBehaviourException();
                }
            }
        }
    }

    public NodeInterface lookup(Long id) throws IOException, TimerExpiredException {
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
     * @param pred previous nodeId
     * @param index nodeId of node to check
     * @param succ successor nodeId
     * @return return true if index is between pred(excluded) and succ(excluded), otherwise return false
     *         (return true if pred == succ)
     */
    private boolean checkInterval(long pred, long index, long succ) {
        if (pred == succ)
            return true;
        if (pred > succ) {
            return (index > pred && index < Math.pow(2, dimFingerTable)) || (index >= 0 && index < succ);
        } else {
            return index > pred && index < succ;
        }
    }

    //Check che nell'intervallo comprende anche l'estremo superiore succ
    //Necessaria in find successor
    private boolean checkIntervalEquivalence(long pred, long index, long succ) {
        if (pred == succ)
            return true;
        if (pred > succ) {
            return (index > pred && index < Math.pow(2, dimFingerTable)) || (index >= 0 && index <= succ);
        } else {
            return index > pred && index <= succ;
        }
    }

    //Ritorna FALSE in caso di pred e succ uguali
    //Chiamato solo in closestPrecedingNode per evitare loop infinito
    private boolean checkIntervalClosest(long pred, long index, long succ) {
        if (pred == succ)
            return false;
        if (pred > succ) {
            //controllate se il >=0 ha senso, l'ho messo in tutte e 3 le check
            return (index > pred && index < Math.pow(2, dimFingerTable)) || (index >= 0 && index < succ);
        } else {
            return index > pred && index < succ;
        }
    }

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

    private void startSocketListener(int socketPort) {
        SocketNodeListener socketNodeListener = new SocketNodeListener(this, socketPort);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    public Long hash(String ipAddress, int socketPort) {
        Long ipNumber = ipToLong(ipAddress) + socketPort;
        Long numberNodes = (long) Math.pow(2, dimFingerTable);
        return ipNumber % numberNodes;
    }

    private long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);
        }
        return result;
    }

    //CONTROLLER

    private void openController() {
        try {
            this.socketController = new Socket(ipController, portController);
            //this.socketController = new Socket("127.0.0.1", 59898);
            this.sendToController("#Connected");
        } catch (Exception e) {
            out.println("ERRORE CONTROLLER");
        }
    }

    @Override
    public void sendToController(String text) {
        try {
            this.outBuffer = new PrintWriter(this.socketController.getOutputStream(), true);
            outBuffer.println(this.nodeId + text);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void updateStable(boolean stable) {
        if (this.stable != stable) {
            this.stable = stable;
            if (!this.stable) {
                this.sendToController("#NotStable");
            } else {
                this.sendToController("#Stable");
            }
        }
    }

    //KEY-VALUE

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

    public void addKeyToStore(Map.Entry<Long, Object> keyValue) {
        keyStore.put(keyValue.getKey(), keyValue.getValue());
    }

    public Object retrieveKeyFromStore(Long key) {
        return keyStore.get(key);
    }

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

    @Override
    public Object findKey(Long key) throws IOException, TimerExpiredException {
        long hashKey = key % (long) Math.pow(2, dimFingerTable);
        if (successorList.get(0).equals(this))
            return keyStore.get(key);

        if (predecessor != null && checkIntervalEquivalence(predecessor.getNodeId(), hashKey, nodeId))
            return keyStore.get(key);

        return findSuccessor(hashKey).findKey(key);
    }


    public void leave() throws IOException, ConnectionErrorException {
        out.println("Now I'm leaving\n\n");
        transferKey();
        UpdateNode.stopUpdate();
        SocketNodeListener.setUpdate(false);

        successorList.get(0).updateAfterLeave(nodeId, predecessor);

        predecessor.updateAfterLeave(nodeId, successorList.get(successorList.size() - 1));

    }

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

    public synchronized void updateAfterLeave(Long oldNodeID, NodeInterface newNode)
            throws IOException, ConnectionErrorException {
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
     * @return .
     */
    @Override
    public int getInitialSocketPort() {
        throw new UnexpectedBehaviourException();
    }

    /**
     * Not used in this class
     *
     * @param socketPort .
     */
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
     * @return .
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