package node;

import exceptions.ConnectionErrorException;
import network.SocketNodeListener;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private static final int DIM_FINGER_TABLE = 3; //questo poi potremmo metterlo variabile scelto nella create
    private int next;

    public Node(String ipAddress) {
        this.ipAddress = ipAddress;
        this.nodeId = 0;
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
        if (checkInterval(nodeId, id, successor.getNodeId())) {
            return this.successor;
        } else {
            next = closestPrecedingNode(id);
            if (this == next)
                return successor;
            return next.findSuccessor(id);
        }
    }

    @Override
    public NodeInterface closestPrecedingNode(long id) {
        long nodeIndex;
        for (int i = DIM_FINGER_TABLE - 1; i >= 0; i--) {
            nodeIndex = fingerTable.get(i).getNodeId();
            if (checkInterval(nodeIndex, id, nodeId))
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
            return (index > pred && index < Math.pow(2, DIM_FINGER_TABLE) - 1) || (index > 0 && index < succ);
        } else {
            return index > pred && index < succ;
        }
    }

    @Override
    public void fixFingers() throws IOException {
        long idToFind;
        next = next + 1;
        if (next > DIM_FINGER_TABLE)
            next = 0;
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

    static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    private String hash(String ipAddress) {
        return "";
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

    public Map<Integer, NodeInterface> getFingerTable() {
        return fingerTable;
    }

    public static void main(String[] args) throws IOException {
        Node node0 = new Node(0);
        Node node1 = new Node(1);
        Node node5 = new Node(5);

        node0.create();
        node1.join(node0);
        node5.join(node1);
        out.println("0:");
        //out.println(node0.getPredecessor().getNodeId());
        out.println(node0.getSuccessor().getNodeId());
        out.println("1:");
        //out.println(node1.getPredecessor().getNodeId());
        out.println(node1.getSuccessor().getNodeId());
        out.println("5:");
        //out.println(node5.getPredecessor().getNodeId());
        out.println(node5.getSuccessor().getNodeId());

        node0.notify(node1);
        node1.notify(node0);
        node0.notify(node5);
        node5.notify(node1);
        node0.stabilize();
        node1.stabilize();
        node5.stabilize();
        node0.stabilize();
        node0.stabilize();
        out.println("PREDECESSORE\nSUCCESSORE");
        out.println("0:----------------------");
        out.println(node0.getPredecessor().getNodeId());
        out.println(node0.getSuccessor().getNodeId());
        out.println("1:-----------------------");
        out.println(node1.getPredecessor().getNodeId());
        out.println(node1.getSuccessor().getNodeId());
        out.println("5:-----------------------");
        out.println(node5.getPredecessor().getNodeId());
        out.println(node5.getSuccessor().getNodeId());
        out.println("\n\nFINGER TABLE");

        for (int i = 0; i < 10; i++) {
            node0.fixFingers();
            node0.fixFingers();
            node0.fixFingers();
            node1.fixFingers();
            node1.fixFingers();
            node1.fixFingers();
            node5.fixFingers();
            node5.fixFingers();
            node5.fixFingers();
        }

        out.println("Pavoletti\n\n");
        out.println("Nodo 0: ------------------------");
        out.println(node0.getFingerTable().get(0).getNodeId());
        out.println(node0.getFingerTable().get(1).getNodeId());
        out.println(node0.getFingerTable().get(2).getNodeId());
        out.println("Nodo 1: ------------------------");
        out.println(node1.getFingerTable().get(0).getNodeId());
        out.println(node1.getFingerTable().get(1).getNodeId());
        out.println(node1.getFingerTable().get(2).getNodeId());
        out.println("Nodo 5: ------------------------");
        out.println(node5.getFingerTable().get(0).getNodeId());
        out.println(node5.getFingerTable().get(1).getNodeId());
        out.println(node5.getFingerTable().get(2).getNodeId());
    }
}