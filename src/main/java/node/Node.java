package node;

import exceptions.ConnectionErrorException;
import exceptions.NodeIdAlreadyExistsException;
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
    private transient int dimFingerTable = 3;
    private transient int dimSuccessorList = 3; //todo quanto è lunga la lista?
    private transient int nextFinger;
    private transient volatile ConcurrentHashMap<Long, Object> keyStore;
    //connection handler
    private transient volatile SocketManager socketManager;

    private transient volatile PrintWriter outBuffer;
    private transient volatile Socket socketController;
    private transient volatile boolean stable = true;

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
        this.ipAddress = ipAddress;
        this.socketPort = socketPort;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.dimFingerTable = dimFingerTable;
        this.nodeId = hash(ipAddress, socketPort);
        this.socketManager = null;
        this.keyStore = new ConcurrentHashMap();
        this.nextFinger = 0;
    }

    private void OpenController (){
        try{
            this.socketController = new Socket("127.0.0.1", 59898);
            this.sendToController("#Connected");
        } catch (Exception e){
            out.println("ERRORE CONTROLLER");
        }
    }

    public void sendToController(String text) {
        try {
            this.outBuffer = new PrintWriter(this.socketController.getOutputStream(), true);
            outBuffer.println(this.nodeId + text);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void updateStable(boolean listSucc, boolean listFiger){
        boolean local;
        if (listSucc && listFiger){
            local = true;
        }
        else{
            local = false;
        }
        if (stable != local){
            stable = local;
            if (!stable){
                this.sendToController("#NotStable");
            }
            else{
                this.sendToController("#Stable");
            }
        }
    }


    public void create(int m) {
        dimFingerTable = m;
        nodeId = hash(ipAddress, socketPort);
        out.println("MIO ID: " + nodeId);
        createSuccessorList();
        startSocketListener(socketPort);
        createFingerTable();
        socketManager = new SocketManager(this);
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
        OpenController ();


    }

    public void join(String joinIpAddress, int joinSocketPort)
            throws ConnectionErrorException, NodeIdAlreadyExistsException, IOException {
        startSocketListener(socketPort);
        createFingerTable();

        NodeCommunicator nodeTemp = new NodeCommunicator(joinIpAddress, joinSocketPort, this, hash(joinIpAddress, joinSocketPort)); // crea un nodecomunicator temporaneo.
        dimFingerTable = nodeTemp.getDimFingerTable();
        this.nodeId = hash(ipAddress, socketPort);
        out.println("NODE ID: " + nodeId);

        createSuccessorList();
        this.socketManager = new SocketManager(this);
        NodeInterface successorNode = nodeTemp.findSuccessor(this.nodeId);
        if (successorNode.getNodeId().equals(nodeId)) //se find successor ritorna un nodo con lo stesso tuo id significa che esiste già un nodo con il tuo id
            throw new NodeIdAlreadyExistsException();

        nodeTemp.close();
        OpenController ();

        successorList.set(0, socketManager.createConnection(successorNode)); //creo nuova connessione
        initializeSuccessorList();
        successorList.get(0).notify(this); //serve per settare il predecessore nel successore del nodo


        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    private void initializeSuccessorList() throws IOException {
        List<NodeInterface> successorNodeList = successorList.get(0).getSuccessorList();
        for (NodeInterface node: successorNodeList) {
            if (node.getNodeId().equals(successorList.get(0).getNodeId()) || node.getNodeId().equals(this.nodeId))
                break;
            if (successorList.size() < dimSuccessorList){
                try {
                    successorList.add(socketManager.createConnection(node));
                } catch (ConnectionErrorException e) {
                    throw new UnexpectedBehaviourException();
                }
            }
        }
    }

    synchronized void listStabilize() throws IOException {

        //questo serve per settare il primo successore
        NodeInterface x = successorList.get(0).getPredecessor();
        if (x == null) {
            successorList.get(0).notify(this);
            return;
        }
        long nodeIndex = x.getNodeId();
        long oldSucID = successorList.get(0).getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID)) {
            try{
                socketManager.closeCommunicator(oldSucID);
                successorList.set(0, socketManager.createConnection(x));
            }
            catch (ConnectionErrorException e){
                throw new UnexpectedBehaviourException();
            }
        }
        successorList.get(0).notify(this);

        boolean already = false;

        List<NodeInterface> xList; //xList contiene la lista dei successori del successore
        xList = successorList.get(0).getSuccessorList();
        if (successorList.size() < dimSuccessorList){
            for (NodeInterface xNode: xList) {
                if (!xNode.getNodeId().equals(nodeId) && successorList.size() < dimSuccessorList) {
                    try {
                        for (NodeInterface internalNode: successorList ) {
                            if (internalNode.getNodeId().equals(xNode.getNodeId())){
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
        }
        else {
            for (int i = 1; i < dimSuccessorList && i < xList.size(); i++) {
                if (!successorList.get(i).getNodeId().equals(xList.get(i - 1).getNodeId())
                        && !xList.get(i-1).getNodeId().equals(nodeId)){
                    try {
                        socketManager.closeCommunicator(successorList.get(i).getNodeId());
                        successorList.set(i, socketManager.createConnection(xList.get(i-1)));
                    } catch (ConnectionErrorException e) {
                        throw new UnexpectedBehaviourException();
                    }
                }
            }
        }
    }
    //catch successor exception--->update listSuccessor

    @Override
    public NodeInterface findSuccessor(Long id) throws IOException {
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

        for (int i = successorList.size()-1; i>=0; i-- ){
            nodeIndex = successorList.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, this.nodeId)) {
                maxClosestId = nodeIndex;
                maxClosestNode=successorList.get(i);
                break;
            }
        }

        for (int i = dimFingerTable - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, this.nodeId))
                if (checkInterval3(maxClosestId,nodeIndex,id))
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
        }
        else {
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

    public NodeInterface lookup(Long id) throws IOException {
        for (NodeInterface nodeInterface : successorList)
            if (id.equals(nodeInterface.getNodeId()))
                return nodeInterface;
        if (id.equals(predecessor.getNodeId()))
            return predecessor;
        else
            return findSuccessor(id);
    }

    synchronized void fixFingers() throws IOException {
        long idToFind;
        nextFinger = nextFinger + 1;
        if (nextFinger > dimFingerTable)
            nextFinger = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, nextFinger - 1))) % (long) Math.pow(2, dimFingerTable);
        NodeInterface node = findSuccessor(idToFind);
        if (!node.getNodeId().equals(fingerTable.get(nextFinger - 1).getNodeId())){ //se il nuovo nodo è diverso da quello già presente
            NodeInterface newConnection;
            try {
                socketManager.closeCommunicator(fingerTable.get(nextFinger -1).getNodeId());//chiudo connessione verso il vecchio nodo
                newConnection = socketManager.createConnection(node);
                fingerTable.replace(nextFinger -1, newConnection);
            } catch (ConnectionErrorException e) {
                throw new UnexpectedBehaviourException();
            }
        }
        //else non faccio niente, perchè il vecchio nodo della finger è uguale a quello nuovo
    }

    private boolean checkInterval(long pred, long index, long succ) {
        if (pred == succ)
            return true;
        if (pred > succ) {
            return (index > pred && index < Math.pow(2, dimFingerTable) ) || (index >= 0 && index < succ);
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
            return (index > pred && index < Math.pow(2, dimFingerTable) ) || (index >= 0 && index <= succ);
        } else {
            return index > pred && index <= succ;
        }
    }

    //Ritorna FALSE in caso di pred e succ uguali
    //Chiamato solo in closestPrecedingNode per evitare loop infinito
    private boolean checkInterval3(long pred, long index, long succ) {
        if (pred == succ)
            return false;
        if (pred > succ) {
            //controllate se il >=0 ha senso, l'ho messo in tutte e 3 le check
            return (index > pred && index < Math.pow(2, dimFingerTable) ) || (index >= 0 && index < succ);
        } else {
            return index > pred && index < succ;
        }
    }

    public void checkDisconnectedNode(Long disconnectedId){
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

    //The finger table is initialized with this in all positions
    private void createFingerTable() {
        for (int i = 0; i < dimFingerTable; i++)
            fingerTable.put(i, this);
    }

    //The successor list is initialized with only this
    private void createSuccessorList(){
        successorList = new CopyOnWriteArrayList<>();
        successorList.add(0, this);
    }

    @Override
    public Long getNodeId() {
        return nodeId;
    }

    @Override
    public void close() throws IOException {
        //do nothing
    }

    @Override
    public NodeInterface getPredecessor() {
        return predecessor;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public int getSocketPort() {
        return socketPort;
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

    public Map<Integer, NodeInterface> getFingerTable() {
        return fingerTable;
    }

    public Long hash(String ipAddress, int socketPort) {
        Long ipNumber = ipToLong(ipAddress) + socketPort;
        Long numberNodes = (long)Math.pow(2, dimFingerTable);
        return ipNumber%numberNodes;
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
        if (successorList!=null)
            for (NodeInterface nodeInterface : successorList)
                string = string + "\t" + nodeInterface.getNodeId();
        string = string + "\n\n" +
                "FINGER TABLE:\n";
        for (int i = 0;i<fingerTable.size() && i< dimFingerTable; i++)
            string = string +
                    "\t\t" + fingerTable.get(i).getNodeId() + "\n";
        //KEY
        string = string + "MY KEY" + "\n";
        for (Map.Entry<Long, Object> keyValue:
             keyStore.entrySet()) {
            string = string + keyValue.getKey() + " " + keyValue.getValue() + "\n";
        }

        string = string + "--------------------------\n";
        return string;
    }
    //KEY-VALUE

    public NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws IOException {
        Long hashKey = keyValue.getKey()%(long)Math.pow(2, dimFingerTable);
        if (hashKey.equals(this.nodeId) || successorList.get(0).equals(this)){
            addKeyToStore(keyValue);
            return this;
        }
        NodeInterface newNodeKey;
      /*  for (int i = 0; i < successorList.size(); i++) {
            if (keyValue.getKey().equals(successorList.get(i).getNodeId())) {
                newNodeKey = successorList.get(i);
                newNodeKey.addKey(keyValue);
                return newNodeKey;
            }
        }*/
        if (predecessor!= null && hashKey.equals(predecessor.getNodeId()))
            newNodeKey= predecessor;
        else
            newNodeKey = findSuccessor(hashKey);

        if (newNodeKey.getNodeId().equals(nodeId)) {
            addKeyToStore(keyValue);
            return this;
        }

        newNodeKey.addKey(keyValue);
        return newNodeKey;
    }

    public void addKeyToStore(Map.Entry<Long, Object> keyValue){
        keyStore.put(keyValue.getKey(),keyValue.getValue());
    }

    public Object retrieveKeyFromStore(Long key){
        return keyStore.get( key);
    }

    public void moveKey() throws IOException {
        for (Map.Entry<Long, Object> keyValue:
             keyStore.entrySet()) {
            Long hashKey = keyValue.getKey()%(long)Math.pow(2, dimFingerTable);
            if (checkIntervalEquivalence(this.nodeId, hashKey, predecessor.getNodeId())) {
                predecessor.addKey(new AbstractMap.SimpleEntry<>(keyValue.getKey(),keyValue.getValue()));
                keyStore.remove(keyValue.getKey());
            }
        }
    }

    @Override
    public Object findKey(Long key) throws IOException {
        Long hashKey = key%(long)Math.pow(2, dimFingerTable);
        if (successorList.get(0).equals(this))
            return keyStore.get(key);

        if (predecessor!= null && checkIntervalEquivalence(predecessor.getNodeId(), hashKey, nodeId))
            return keyStore.get(key);

        return findSuccessor(hashKey).findKey(key);
    }

}