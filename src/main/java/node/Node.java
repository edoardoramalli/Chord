package node;

import exceptions.ConnectionErrorException;
import exceptions.UnexpectedBehaviourException;
import network.NodeCommunicator;
import network.SocketManager;
import network.SocketNodeListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static java.lang.System.*;

public class Node implements NodeInterface, Serializable {
    private String ipAddress;
    private int socketPort;
    private Long nodeId;
    private transient volatile NodeInterface successor;
    private transient volatile NodeInterface predecessor;
    private transient volatile Map<Integer, NodeInterface> fingerTable;
    private transient List<NodeInterface> successorList;
    private transient int dimFingerTable = 3;
    private transient int next;
    //connection handler
    private transient volatile SocketManager socketManager;

    private static final long serialVersionUID = 1L;

    public Node(String ipAddress, int socketPort) {
        this.ipAddress = ipAddress;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.socketPort = socketPort;
        this.next = 0;
        this.nodeId = hash(ipAddress);
        this.socketManager = new SocketManager(this);
    }

    public void create(int m) {
        successor = this;
        predecessor = null;
        dimFingerTable = m;
        startSocketListener(socketPort);
        createFingerTable();
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    public void join(String joinIpAddress, int joinSocketPort) throws ConnectionErrorException, IOException {
        NodeCommunicator node = new NodeCommunicator(joinIpAddress, joinSocketPort, this, hash(joinIpAddress));
        predecessor = null;
        NodeInterface successorNode = node.findSuccessor(this.nodeId);
        dimFingerTable = node.getDimFingerTable();
        node.close();
        successor = socketManager.createConnection(successorNode); //creo nuova connessione
        successor.notify(this); //serve per settare il predecessore nel successore del nodo
        startSocketListener(socketPort);
        createFingerTable();
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    void stabilize() throws IOException {
        //controllo su null predecessor
        NodeInterface x = successor.getPredecessor();
        long nodeIndex = x.getNodeId();
        long oldSucID = successor.getNodeId();
        //se x == successor non entro neanche nell'if
        if (checkInterval(getNodeId(), nodeIndex, oldSucID) && !x.getNodeId().equals(successor.getNodeId())) {
            try {
                socketManager.closeCommunicator(successor.getNodeId());
                successor = socketManager.createConnection(x);
            } catch (ConnectionErrorException e) {
                e.printStackTrace();
            }
        }
        successor.notify(this); //questa forse va dentro l'if, perché se non cambio il successore non ho bisogno di fargli la notify
    }

    @Override
    public NodeInterface findSuccessor(Long id) throws IOException {
        NodeInterface nextNode;
        if (checkIntervalEquivalence(nodeId, id, successor.getNodeId())) {
            return successor;
        } else {
            nextNode = closestPrecedingNode(id);
            if (this == nextNode)
                return this;
            return nextNode.findSuccessor(id);
        }
    }

    private NodeInterface closestPrecedingNode(Long id){
        long nodeIndex;
        for (int i = dimFingerTable - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, nodeId))
                return fingerTable.get(i);
        }
        return this;
    }

    @Override
    public synchronized void notify(NodeInterface n) throws IOException {
        if (predecessor == null) {
            try {
                predecessor = socketManager.createConnection(n); //creo connessione aumentando di 1
            } catch (ConnectionErrorException e) {
                e.printStackTrace();
            }
        }
        else {
            long index = n.getNodeId();
            long predIndex = predecessor.getNodeId();
            if (checkInterval(predIndex, index, getNodeId()) && !(predecessor.getNodeId().equals(n.getNodeId()))) { //entro solo se n è diverso dal predecessore
                //closeCommunicator(predecessor.getHostId());
                try {
                    socketManager.closeCommunicator(predecessor.getNodeId());//chiudo connessione verso vecchio predecessore
                    predecessor = socketManager.createConnection(n); //apro connessione verso nuovo predecessore
                } catch (ConnectionErrorException e) {
                    e.printStackTrace();
                }
            }
        }
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

    synchronized void fixFingers() throws IOException {
        long idToFind;
        next = next + 1;
        if (next > dimFingerTable)
            next = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, next - 1))) % (long) Math.pow(2, dimFingerTable);
        NodeInterface node = findSuccessor(idToFind);
        if (!node.getNodeId().equals(fingerTable.get(next - 1).getNodeId())){ //se il nuovo nodo è diverso da quello già presente
            NodeInterface newConnection;
            try {
                newConnection = socketManager.createConnection(node);
            } catch (ConnectionErrorException e) {
                throw new UnexpectedBehaviourException();
            }
            socketManager.closeCommunicator(fingerTable.get(next-1).getNodeId());//chiudo connessione verso il vecchio nodo
            fingerTable.replace(next-1, newConnection);
        }
        //else non faccio niente, perchè il vecchio nodo della finger è uguale a quello nuovo
    }

    //TODO da implementare
    @Override
    public void checkPredecessor() {

    }

    public void checkDisconnectedNode(Long disconnectedId){
        if (successor.getNodeId().equals(disconnectedId)) //se il nodo disconnesso è il successore lo metto = this
            successor = this;
        if (predecessor.getNodeId().equals(disconnectedId)) //se il nodo disconnesso è il predecessore lo metto = null
            predecessor = null;
        for (int i = 0; i < dimFingerTable; i++) //se il nodo disconnesso è uno della finger lo metto = this
            if (fingerTable.get(i).getNodeId().equals(disconnectedId))
                fingerTable.replace(i, this);
    }

    private void startSocketListener(int socketPort) {
        SocketNodeListener socketNodeListener = new SocketNodeListener(this, socketPort);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    private void createFingerTable() {
        for (int i = 0; i < dimFingerTable; i++)
            fingerTable.put(i, this);
    }

    public NodeInterface lookup(Long id) throws IOException {
        if (id.equals(successor.getNodeId())) {
            return successor;
        } else if (id.equals(predecessor.getNodeId())) {
            return predecessor;
        } else {
            return findSuccessor(id);
        }
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
    public SocketManager getSocketManager() {
        return socketManager;
    }

    public Long hash(String ipAddress) {
        Long ipNumber = ipToLong(ipAddress);
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
                "SUCCESSOR:\t\t" + successor.getNodeId() + "\n\n" +
                "FINGER TABLE:\n";
        for (int i = 0; i< dimFingerTable; i++)
            string = string +
                    "\t\t" + fingerTable.get(i).getNodeId() + "\n";
        string = string + "--------------------------\n";
        return string;
    }

    //TODO da qui dobbiamo collegare tutto

    public void listStabilize() throws ConnectionErrorException, IOException {

        NodeInterface x = successor.getPredecessor();
        long nodeIndex = x.getNodeId();
        long oldSucID = successor.getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID))
            successor = x;
        List<NodeInterface> xList = new ArrayList<>();
        // xList=successor.getSuccessorList();
        successorList = copySuccessorList(xList);

        successor.notify(this);
    }
    //catch successor exception--->update listSuccessor

    private List<NodeInterface> copySuccessorList(List<NodeInterface> nextNodeSuccessorList) throws ConnectionErrorException, IOException {
        ArrayList<NodeInterface> newSuccessorList = new ArrayList<>();
        String ip = successor.getIpAddress();
        int port = 0;
        newSuccessorList.add(new NodeCommunicator(ip, port, this, hash(ip)));
        for (int i = 0; i < nextNodeSuccessorList.size()-1; i++) {
            ip = nextNodeSuccessorList.get(i).getIpAddress();
            port = nextNodeSuccessorList.get(i).getSocketPort();
            newSuccessorList.add(socketManager.createConnection(new NodeCommunicator(ip, port, this, hash(ip))));
        }
        return newSuccessorList;
    }

    public NodeInterface closestPrecedingNodeList(long id) {
        long nodeIndex;
        long maxClosestId=this.nodeId;
        NodeInterface maxClosestNode=this;

        for (int i = successorList.size()-1; i>=0; i-- ){
            nodeIndex = successorList.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, this.nodeId)) {
                maxClosestId = nodeIndex;
                break;
            }
        }

        for (int i = dimFingerTable - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, this.nodeId))
                if (maxClosestId<=nodeIndex)
                    return fingerTable.get(i);
                else
                    break;
        }
        return maxClosestNode;
    }

    @Override
    public List<NodeInterface> getSuccessorList() {
        return successorList;
    }
}