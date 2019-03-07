package node;

import exceptions.ConnectionErrorException;
import network.NodeCommunicator;
import network.SocketNodeListener;

import java.io.IOException;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.Executors;

import static java.lang.System.out;



public class Node implements NodeInterface {
    private String ipAddress;
    private long nodeId;
    private volatile NodeInterface successor;
    private volatile NodeInterface predecessor;
    private volatile Map<Integer, NodeInterface> fingerTable;
    private int dimFingerTable;
    private int next;

    public Node(String ipAddress) {
        this.ipAddress = ipAddress;
        this.nodeId = hash(ipAddress);
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.next = 0;
    }

    public void create(int mySocketPort, int m) {
        successor = this;
        predecessor = null;
        dimFingerTable = m;
        startSocketListener(mySocketPort);
        createFingerTable();
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    public void join(int mySocketPort, String ipAddress, int socketPort) throws ConnectionErrorException, IOException {
        NodeCommunicator node = new NodeCommunicator(ipAddress, socketPort);
        predecessor = null;
        successor = node.findSuccessor(this.nodeId);
        successor.notify(this); //serve per settare il predecessore nel successore del nodo
        node.close();
        startSocketListener(mySocketPort);
        createFingerTable();
        Executors.newCachedThreadPool().submit(new UpdateNode(this));
    }

    @Override
    public void stabilize() {
        //controllo su null predecess
        NodeInterface x = successor.getPredecessor();
        long nodeIndex = x.getNodeId();
        long oldSucID = successor.getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID))
            successor = x;
        successor.notify(this);
    }



    @Override
    public NodeInterface findSuccessor(long id) throws IOException {
        NodeInterface next;
        if (checkIntervalEquivalence(nodeId, id, successor.getNodeId())) {
            return successor;
        } else {
            next = closestPrecedingNode(id);
            //non ritorniamo più successor ma this
            if (this == next)
                return this;
            return next.findSuccessor(id);
        }
    }

    @Override
    public NodeInterface closestPrecedingNode(long id) {
        long nodeIndex;
        for (int i = dimFingerTable - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkInterval3(nodeIndex, id, nodeId))
                return fingerTable.get(i);
        }
        return this;
    }

    @Override
    public void notify(Node n) {
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

    @Override
    public void fixFingers() throws IOException {
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

    private String lookup(long id) {
        if (id == successor.getNodeId()) {
            return successor.getIpAddress();
        } else if (id == predecessor.getNodeId()) {
            return predecessor.getIpAddress();
        } else {
            return closestPrecedingNode(id).getIpAddress();
        }
    }

    private long hash(String ipAddress) {
        long ipNumber = ipToLong(ipAddress);
        long numberNodes = (long)Math.pow(2, dimFingerTable);
        return ipNumber%numberNodes;
    }


    @Override
    public long getNodeId() {
        return nodeId;
    }

    @Override
    public NodeInterface getSuccessor() {
        return successor;
    }

    @Override
    public NodeInterface getPredecessor() {
        return predecessor;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    public long ipToLong(String ipAddress) {

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

    private void printFingerTable(){
        out.println("FINGER TABLE: " + nodeId);
        for (int i = 0; i< dimFingerTable; i++)
        {
            out.println(fingerTable.get(i).getNodeId());
        }
    }

    private void printPredecessorAndSuccessor(){
        out.println(nodeId + ":----------------------");
        out.println(predecessor.getNodeId());
        out.println(successor.getNodeId());
    }

    private NodeInterface lookup2(long id) {
        if (id == successor.getNodeId()) {
            return successor;
        } else if (id == predecessor.getNodeId()) {
            return predecessor;
        } else {
            return closestPrecedingNode(id);
        }
    }



    /*public static void main(String[] args) throws IOException {

        int m = 3;
        String ip0 = "192.168.1.0";
        String ip0 = "192.168.1.0";
        String ip0 = "192.168.1.0";
        String ip0 = "192.168.1.0";
        String ip0 = "192.168.1.0";

        Node node0 = new Node("192.168.1.0");
        Node node1 = new Node("192.168.1.1");
        Node node2 = new Node("192.168.1.2");
        Node node3 = new Node("192.168.1.3");
        Node node4 = new Node("192.168.1.4");
        Node node6 = new Node("192.168.1.6");
        Node node7 = new Node("192.168.1.7");

        node0.create(m);
        node1.join(node0);
        node6.join(node1);
        node4.join(node1);
        node2.join(node4);
        node3.join(node4);
        node7.join(node2);

        out.println("PREDECESSORE\nSUCCESSORE");

        node0.printPredecessorAndSuccessor();
        node1.printPredecessorAndSuccessor();
        node2.printPredecessorAndSuccessor();
        node3.printPredecessorAndSuccessor();
        node4.printPredecessorAndSuccessor();
        node6.printPredecessorAndSuccessor();
        node7.printPredecessorAndSuccessor();

        node0.printFingerTable();
        node1.printFingerTable();
        node2.printFingerTable();
        node3.printFingerTable();
        node4.printFingerTable();
        node6.printFingerTable();
        node7.printFingerTable();
    }
    */
}