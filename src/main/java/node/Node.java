package node;

import exceptions.ConnectionErrorException;
import network.NodeCommunicator;
import network.SocketNodeListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.Executors;

import static java.lang.System.out;


public class Node implements NodeInterface, Serializable {
    private String ipAddress;
    private int socketPort;
    private transient Long nodeId;
    private transient volatile NodeInterface successor;
    private transient volatile NodeInterface predecessor;
    private transient volatile Map<Integer, NodeInterface> fingerTable;
    private transient int dimFingerTable = 3;
    private transient int next;

    public Node(String ipAddress, int sockeP) {
        this.ipAddress = ipAddress;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.socketPort = sockeP;
        this.next = 0;
        this.nodeId = hash(ipAddress,socketPort);

    }

    public void create(int m) {
        successor = this;
        predecessor = null;
        dimFingerTable = m;
            out.println("NODE ID: " + nodeId);
        startSocketListener(socketPort);
        createFingerTable();
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    public void join(String joinIpAddress, int joinSocketPort) throws ConnectionErrorException, IOException {
            out.println("NODE ID: " + nodeId);
        NodeCommunicator node = new NodeCommunicator(joinIpAddress, joinSocketPort, this);
        predecessor = null;
            node.notify(this);
        NodeInterface successorNode = node.findSuccessor(this.nodeId);
        dimFingerTable = node.getDimFingerTable();
            out.println("NODO SUCCESSORE: ------------------");
            out.println("IP: " + successorNode.getIpAddress());
            out.println("PORT: " + successorNode.getSocketPort());
            out.println("FINE NODO SUCCESSORE: ------------------");
        node.close();
            out.println("DOPO CLOSE");
        successor = new NodeCommunicator(successorNode.getIpAddress(), successorNode.getSocketPort(), this);
            out.println("DOPO SUCCESSORE");
            out.println(successor.getNodeId());
            out.println(successor.getDimFingerTable());
            //TODO MUORE QUI
        successor.notify(this); //serve per settare il predecessore nel successore del nodo
            out.println("DOPO NOTIFY");
        startSocketListener(socketPort);
        createFingerTable();
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    void stabilize() throws IOException {
        //controllo su null predecess
        NodeInterface x = successor.getPredecessor();
        long nodeIndex = x.getNodeId();
        long oldSucID = successor.getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID))
            successor = x;
        successor.notify(this);
    }

    @Override
    public void close() throws IOException{

    }

    @Override
    public NodeInterface findSuccessor(Long id) throws IOException {
        NodeInterface nextNode;
        if (checkIntervalEquivalence(nodeId, id, successor.getNodeId())) {
            return successor;
        } else {
            nextNode = closestPrecedingNode(id);
            //non ritorniamo più successor ma this
            if (this == nextNode)
                return this;
            return nextNode.findSuccessor(id);
        }
    }

    @Override
    public NodeInterface closestPrecedingNode(Long id) throws IOException {
        long nodeIndex;
        for (int i = dimFingerTable - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, nodeId))
                return fingerTable.get(i);
        }
        return this;
    }

    @Override
    public void notify(NodeInterface n) throws IOException{
        if (predecessor == null)
            predecessor = n;
        else {
            long index = n.getNodeId();
            long predIndex = predecessor.getNodeId();
            if (checkInterval(predIndex, index, getNodeId()))
                predecessor = n;
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

    void fixFingers() throws IOException {
        long idToFind;
        next = next + 1;
        if (next > dimFingerTable)
            next = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, next - 1))) % (long) Math.pow(2, dimFingerTable);
        fingerTable.replace(next - 1, findSuccessor(idToFind));
    }

    @Override
    public void checkPredecessor() {

    }

    private void startSocketListener(int socketPort) {
        SocketNodeListener socketNodeListener = new SocketNodeListener(this, socketPort);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    private void createFingerTable() {
        for (int i = 0; i <= dimFingerTable - 1; i++) {
            fingerTable.put(i, this);
        }
    }

    private String lookup(long id) throws IOException {
        if (id == successor.getNodeId()) {
            return successor.getIpAddress();
        } else if (id == predecessor.getNodeId()) {
            return predecessor.getIpAddress();
        } else {
            return closestPrecedingNode(id).getIpAddress();
        }
    }

    public Long hash(String ipAddress, int socketP) {
        Long ipNumber = ipToLong(ipAddress) + socketP;
        Long numberNodes = (long)Math.pow(2, dimFingerTable);
        return ipNumber%numberNodes;
    }

    @Override
    public Long getNodeId() {
        return nodeId;
    }

    @Override
    public NodeInterface getSuccessor() {
        return successor;
    } //TODO questo serve?

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

    public Map<Integer, NodeInterface> getFingerTable() {
        return fingerTable;
    }

    public void printFingerTable() throws IOException {
        out.println("FINGER TABLE: " + nodeId);
        for (int i = 0; i< dimFingerTable; i++)
        {
            out.println(fingerTable.get(i).getNodeId());
        }
    }

    public void printPredecessorAndSuccessor() throws IOException {
        out.println(nodeId + ":----------------------");
        if (predecessor != null)
            out.println("Predecessor: " + predecessor.getNodeId());
        out.println("Successor: " + successor.getNodeId());
    }

    private NodeInterface lookup2(long id) throws IOException {
        if (id == successor.getNodeId()) {
            return successor;
        } else if (id == predecessor.getNodeId()) {
            return predecessor;
        } else {
            return closestPrecedingNode(id);
        }
    }
}