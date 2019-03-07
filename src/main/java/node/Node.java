package node;

import exceptions.ConnectionErrorException;
import network.NodeCommunicator;
import network.SocketNodeListener;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.*;


import java.io.IOException;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.Executors;

import static java.lang.System.out;

public class Node implements NodeInterface {
    private String ipAddress;
    private long nodeId;
    private NodeInterface successor; //TODO questi dovrebbero essere NodeCommunicator
    private NodeInterface predecessor;
    private Map<Integer, NodeInterface> fingerTable;
    private static final int DIM_FINGER_TABLE = 4; //questo poi potremmo metterlo variabile scelto nella create
    private int next;

    public Node(String ipAddress) {
        this.ipAddress = ipAddress;
        this.nodeId = hash(ipAddress);
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.next = 0;
    }

    public void create() {
        successor = this;
        predecessor = null;
        //startSocketListener(0);
        createFingerTable();
    }

    public void join(String ipAddress, int socketPort) throws ConnectionErrorException, IOException {
        //NodeCommunicator node = new NodeCommunicator(ipAddress, socketPort);
        //predecessor = null;
        //successor = node.findSuccessor(this.nodeId);
        //node.close();//TODO questo serve? magari con qualche condizione
        //out.println("Gol di Pavoletti");
        //startSocketListener(1);
    }

    @Override
    public void stabilize() {
        //controllo su null predecess
        NodeInterface x = successor.getPredecessor();
        long nodeIndex = x.getNodeId();
        long oldSucID = successor.getNodeId();
        if (checkInterval(getNodeId(), nodeIndex, oldSucID))
            successor = x;

        this.successor.notify(this);

    }



    @Override
    public NodeInterface findSuccessor(long id) throws IOException {
        NodeInterface next;
        if (checkIntervalEquivalence(nodeId, id, successor.getNodeId())) {
            return this.successor;
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
        for (int i = DIM_FINGER_TABLE - 1; i >= 0; i--) {
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
            return (index > pred && index < Math.pow(2, DIM_FINGER_TABLE) ) || (index >= 0 && index < succ);
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
            return (index > pred && index < Math.pow(2, DIM_FINGER_TABLE) ) || (index >= 0 && index <= succ);
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
            return (index > pred && index < Math.pow(2, DIM_FINGER_TABLE) ) || (index >= 0 && index < succ);
        } else {
            return index > pred && index < succ;
        }
    }

    @Override
    public void fixFingers() throws IOException {
        long idToFind;
        next = next + 1;
        if (next > DIM_FINGER_TABLE)
            next = 1;
        //fix cast
        idToFind = (nodeId + ((long) Math.pow(2, next - 1))) % (long) Math.pow(2, DIM_FINGER_TABLE);
        fingerTable.replace(next - 1, findSuccessor(idToFind));
    }

    @Override
    public void checkPredecessor() {

    }

    private void startSocketListener(int n) { //TODO devo cercare di togliere questo n
        SocketNodeListener socketNodeListener = new SocketNodeListener(this, n);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    private void createFingerTable() {
        for (int i = 0; i <= DIM_FINGER_TABLE - 1; i++) {
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
        long numberNodes = (long)Math.pow(2,DIM_FINGER_TABLE);
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

    public void handleMessage (String msg){
        //check if the destinatary of the msg it is node itself, otherwise send it to the correct node.
    }

    //------------------- cose


    public Node(long nodeId) {
        this.nodeId = nodeId;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.next = 0;
    }

    public void join(Node node) throws IOException {
        predecessor = null;
        successor = node.findSuccessor(this.nodeId);
        createFingerTable();
        out.println("Gol di Pavoletti");
    }

    public void printFingerTable(){
        System.out.println("FINGER TABLE: " + nodeId);
        for (int i=0; i<DIM_FINGER_TABLE; i++)
        {
            System.out.println(fingerTable.get(i).getNodeId());

        }

    }



    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        Node node0 = new Node("192.168.1.0");
        Node node1 = new Node("192.168.1.1");
        Node node2 = new Node("192.168.1.2");
        Node node3 = new Node("192.168.1.3");
        Node node4 = new Node("192.168.1.4");

        node0.create();
        node1.join(node0);
        node4.join(node1);
        node3.join(node4);
        node2.join(node4);

        node0.notify(node4);
        node1.notify(node0);
        node2.notify(node1);
        node3.notify(node2);
        node4.notify(node4);

        for (int i =0 ; i< 10 ;i++){
            node0.stabilize();
            node1.stabilize();
            node2.stabilize();
            node3.stabilize();
            node4.stabilize();
        }

        out.println("PREDECESSORE\nSUCCESSORE");

        out.println("0:----------------------");
        out.println(node0.getPredecessor().getNodeId());
        out.println(node0.getSuccessor().getNodeId());

        out.println("1:-----------------------");
        out.println(node1.getPredecessor().getNodeId());
        out.println(node1.getSuccessor().getNodeId());

        out.println("2:-----------------------");
        out.println(node2.getPredecessor().getNodeId());
        out.println(node2.getSuccessor().getNodeId());

        out.println("3:-----------------------");
        out.println(node3.getPredecessor().getNodeId());
        out.println(node3.getSuccessor().getNodeId());

        out.println("4:-----------------------");
        out.println(node4.getPredecessor().getNodeId());
        out.println(node4.getSuccessor().getNodeId());

        out.println("AAAAAAAAAAAAA " + node0.findSuccessor(node1.getNodeId()).getNodeId());

        for (int i = 0; i < 10; i++) {
            node0.fixFingers();
            node0.fixFingers();
            node0.fixFingers();

            node1.fixFingers();
            node1.fixFingers();
            node1.fixFingers();
            node1.fixFingers();
            node2.fixFingers();
            node2.fixFingers();
            node2.fixFingers();

            node3.fixFingers();
            node3.fixFingers();
            node3.fixFingers();

            node4.fixFingers();
            node4.fixFingers();
            node4.fixFingers();

        }

        node0.printFingerTable();
        node1.printFingerTable();
        node2.printFingerTable();
        node3.printFingerTable();
        node4.printFingerTable();

        out.println(node0.lookup(3));
    }
}