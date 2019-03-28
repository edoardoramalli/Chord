package node;

import exceptions.ConnectionErrorException;
import network.NodeCommunicator;
import network.SocketManager;
import network.SocketNode;
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
    private transient volatile SocketManager socketManager;
    //private transient volatile Map<Long, NodeInterface> socketManager; //da togliere
    private transient List<NodeInterface> listOfSuccessor = new ArrayList<>();
    private transient int dimFingerTable = 3;
    private transient int next;

    public Node(String ipAddress, int socketPort) {
        this.ipAddress = ipAddress;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.socketPort = socketPort;
        this.next = 0;
        this.nodeId = hash(ipAddress);
        out.println("NODE ID: " + nodeId);
    }

    public void create(int m) {
        socketManager = new SocketManager(this);
        successor = this;
        predecessor = null;
        dimFingerTable = m;
        startSocketListener(socketPort);
        createFingerTable();
        //Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    public void join(String joinIpAddress, int joinSocketPort) throws ConnectionErrorException, IOException {
        socketManager = new SocketManager(this);
        NodeCommunicator node = new NodeCommunicator(joinIpAddress, joinSocketPort, this, hash(joinIpAddress));
        predecessor = null;
        NodeInterface successorNode = node.findSuccessor(this.nodeId);
        dimFingerTable = node.getDimFingerTable();
        err.println(dimFingerTable);
        err.println(successorNode.getIpAddress());
        node.close();
        successor = socketManager.createConnection(successorNode);
        successor.notify(this); //serve per settare il predecessore nel successore del nodo
        //startSocketListener(socketPort);
        createFingerTable();
        //Executors.newCachedThreadPool().submit(new UpdateNode(this));
        out.println("1");
        //stabilize(); //QUI
        out.println("2");
        //fixFingers();
        out.println("3");
        //fixFingers();
        out.println("4");
        //fixFingers();
    }

    void stabilize() throws IOException {
        //controllo su null predecessor
        NodeInterface x = successor.getPredecessor();
        long nodeIndex = x.getHostId();
        long oldSucID = successor.getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID) && !x.getNodeId().equals(successor.getNodeId())) {
            try {
                successor = socketManager.createConnection(x);
            } catch (ConnectionErrorException e) {
                e.printStackTrace();
            }
        }
        successor.notify(this);
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
    public void notify(NodeInterface n) throws IOException {
        if (predecessor == null) {
            try {
                predecessor = socketManager.createConnection(n);
                Executors.newCachedThreadPool().submit(new UpdateNode(this));
                /*stabilize();
                fixFingers();
                fixFingers();
                fixFingers();*/
            } catch (ConnectionErrorException e) {
                e.printStackTrace();
            }
        }
        else {
            long index = n.getNodeId();
            long predIndex = predecessor.getNodeId();
            if (checkInterval(predIndex, index, getNodeId()) && !(predecessor.getNodeId().equals(n.getNodeId()))) {
                //closeCommunicator(predecessor.getHostId());
                try {
                    predecessor = socketManager.createConnection(n);
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

    /*VECCHIA FIX
    void fixFingers() throws IOException {
        long idToFind;
        next = next + 1;
        if (next > dimFingerTable)
            next = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, next - 1))) % (long) Math.pow(2, dimFingerTable);
        fingerTable.replace(next - 1, findSuccessor(idToFind));
    }
     */

    void fixFingers() throws IOException {
        long idToFind;
        next = next + 1;
        if (next > dimFingerTable)
            next = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, next - 1))) % (long) Math.pow(2, dimFingerTable);
        NodeInterface newConnection = null;
        try {
            newConnection = socketManager.createConnection(findSuccessor(idToFind));
        } catch (ConnectionErrorException e) {
            e.printStackTrace();
        }
        //if (!fingerTable.get(next - 1).getHostId().equals(this.nodeId))
        //    closeCommunicator(fingerTable.get(next-1).getHostId());
        fingerTable.replace(next - 1, newConnection);
    }

    //TODO forse va tolto
    @Override
    public void checkPredecessor() {

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

    }

    @Override
    public NodeInterface getSuccessor() {
        return successor;
    } //TODO questo serve?

    @Override
    public NodeInterface getPredecessor() {
        out.println("stampo pred: " + predecessor);
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


    //TODO DA QUI BISOGNA ANCORA COLLEGARE

    private List<NodeInterface> successorList;

    public void listStabilize() throws ConnectionErrorException, IOException {

        NodeInterface x = successor.getPredecessor();
        long nodeIndex = x.getNodeId();
        long oldSucID = successor.getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID))
            successor = x;

        List<NodeInterface> xList=new ArrayList<>();
        // xList=successor.getSuccessorList();
        successorList= copySuccessorList(xList);

        successor.notify(this);
    }
    //catch successor exception--->update listSuccessor

    public List<NodeInterface> copySuccessorList(List<NodeInterface> nextNodeSuccessorList) throws ConnectionErrorException, IOException {
        ArrayList<NodeInterface> newSuccessorList= new ArrayList<>();
        String ip=successor.getIpAddress();
        int port=0;
        newSuccessorList.add(new NodeCommunicator(ip,port, this, hash(ip)));
        for (int i=0; i < nextNodeSuccessorList.size()-1; i++) {

            ip=nextNodeSuccessorList.get(i).getIpAddress();
            port = nextNodeSuccessorList.get(i).getSocketPort();
            newSuccessorList.add(socketManager.createConnection(new NodeCommunicator(ip, port, this, hash(ip))));
        }
        return newSuccessorList;
    }

  /*  public NodeInterface closestPrecedingNodeList(long id) {
        long nodeIndex;
        for (int i = DIM_FINGER_TABLE - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, nodeId))
                return fingerTable.get(i);
        }
        return this;
    }*/

    @Override
    public Long getHostId() {
        return nodeId;
    }

    @Override
    public SocketManager getSocketManager() {
        return socketManager;
    }
}