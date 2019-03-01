package node;

import network.SocketNodeListener;

import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.Executors;

public class Node {
    private String ipAddress;
    private int nodeId;
    private Node successor;
    private Node predecessor;
    private Map<Integer, Node> fingerTable;

    public static final int DIM_FINGER_TABLE = 4;

    public Node(String ipAddress){
        this.ipAddress = ipAddress;
        this.nodeId=0;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
    }

    public int getNodeId() {
        return nodeId;
    }

    public void create(){
        successor = this;
        predecessor = null;
        startSocketListener();
    }

    public void join (Node n){
        predecessor = null;
        successor=n.find_successor(this.nodeId);
    }

    public void stabilize(){

    }

    public Node find_successor(int id){
        Node next;
        if(id > this.getNodeId() && id <= this.successor.getNodeId())
            return this.successor;
        else {
            next = closest_preceding_node(id);
            return next.find_successor(id);
        }
    }

    public Node closest_preceding_node(int id){
        int nodeIndex;
        for (int i=DIM_FINGER_TABLE; i > 1; i--){
            nodeIndex=  fingerTable.get(i).getNodeId();
            if ( nodeIndex < id && nodeIndex > this.getNodeId())
                return  fingerTable.get(i);
        }

        return this;

    }

    public void notify(Node n){

    }

    public void fix_fingers(){

    }

    public void check_predecessor() {

    }

    private void startSocketListener(){
        SocketNodeListener socketNodeListener = new SocketNodeListener(this);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

}
