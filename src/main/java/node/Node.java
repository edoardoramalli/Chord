package node;

import exceptions.ConnectionErrorException;
import network.NodeCommunicator;
import network.SocketNodeListener;

import java.io.IOException;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.Executors;

import static java.lang.System.out;

public class Node implements NodeInterface{
    private String ipAddress;
    private long nodeId;
    private NodeInterface successor;
    private NodeInterface predecessor;
    private Map<Integer, NodeInterface> fingerTable;
    private static final int DIM_FINGER_TABLE = 4; //questo poi potremmo metterlo variabile scelto nella create
    private int next;

    public Node(String ipAddress){
        this.ipAddress = ipAddress;
        this.nodeId=0;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
        this.next=1;
    }

    public void create(){
        //successor = this;
        predecessor = null;
        startSocketListener(0);
    }

    public void join (String ipAddress, int socketPort) throws ConnectionErrorException, IOException {
        NodeCommunicator node = new NodeCommunicator(ipAddress, socketPort);
        predecessor = null;
        successor = node.findSuccessor(this.nodeId);
        node.close();//TODO questo serve? magari con qualche condizione
        out.println("Gol di Pavoletti");
        startSocketListener(1);
    }

    @Override
    public void stabilize(){
        NodeInterface x = this.getSuccessor().getPredecessor();
        long  nodeIndex = x.getNodeId();
        long oldSucID = this.successor.getNodeId();
        if ( nodeIndex < oldSucID && nodeIndex > this.getNodeId())
            this.successor = x;

        this.successor.notify(this);

    }

    @Override
    public NodeInterface findSuccessor(long id) throws IOException {
        NodeInterface next;
        if(id > this.getNodeId() && id <= this.successor.getNodeId())
            return this.successor;
        else {
            next = closestPrecedingNode(id);
            return next.findSuccessor(id);
        }
    }

    @Override
    public NodeInterface closestPrecedingNode(long id){
        long nodeIndex;
        for (int i=DIM_FINGER_TABLE; i > 1; i--){
            nodeIndex=  fingerTable.get(i).getNodeId();
            if ( nodeIndex < id && nodeIndex > this.getNodeId())
                return  fingerTable.get(i);
        }
        return this;
    }

    @Override
    public void notify(Node n){
        long nIndex = n.getNodeId();
        long predIndex = this.predecessor.getNodeId();
        if (this.predecessor == null || (nIndex > predIndex && nIndex < this.getNodeId()) )
            this.predecessor=  n;
    }

    @Override
    public void fixFingers() throws IOException {
        long idToFind;
        next = next + 1;
        if(next > DIM_FINGER_TABLE)
            next=1;
        //fix cast
        idToFind=this.getNodeId() + ((long) Math.pow(2, next-1));
        fingerTable.replace(next,findSuccessor(idToFind));

    }

    @Override
    public void checkPredecessor() {

    }

    private void startSocketListener(int n){ //TODO devo cercare di togliere questo n
        SocketNodeListener socketNodeListener = new SocketNodeListener(this, n);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    public long getNodeId() {
        return nodeId;
    }

    public NodeInterface getSuccessor() {
        return successor;
    }

    public NodeInterface getPredecessor() {
        return predecessor;
    }

}
