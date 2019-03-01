package node;

import exceptions.ConnectionErrorException;
import network.NodeCommunicator;
import network.SocketNode;
import network.SocketNodeListener;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.Executors;

import static java.lang.System.out;

public class Node implements NodeInterface{
    private String ipAddress;
    private int nodeId;
    private Node successor;
    private Node predecessor;
    private Map<Integer, Node> fingerTable;
    private static final int DIM_FINGER_TABLE = 4; //questo poi potremmo metterlo variabile scelto nella create

    public Node(String ipAddress){
        this.ipAddress = ipAddress;
        this.nodeId=0;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = new HashMap<>();
    }

    public void create(){
        successor = this;
        predecessor = null;
        startSocketListener();
    }

    public void join (String ipAddress, int socketPort) throws ConnectionErrorException, IOException {
        NodeCommunicator node = new NodeCommunicator(ipAddress, socketPort);
        predecessor = null;
        successor = node.findSuccessor(this.nodeId);
        node.close();//TODO questo serve? magari con qualche condizione
        out.println("Gol di Pavoletti");
    }

    @Override
    public void stabilize(){

    }

    @Override
    public Node findSuccessor(int id){
        Node next;
        if(id > this.getNodeId() && id <= this.successor.getNodeId())
            return this.successor;
        else {
            next = closestPrecedingNode(id);
            return next.findSuccessor(id);
        }
    }

    @Override
    public Node closestPrecedingNode(int id){
        int nodeIndex;
        for (int i=DIM_FINGER_TABLE; i > 1; i--){
            nodeIndex=  fingerTable.get(i).getNodeId();
            if ( nodeIndex < id && nodeIndex > this.getNodeId())
                return  fingerTable.get(i);
        }
        return this;
    }

    @Override
    public void notify(Node n){

    }

    @Override
    public void fixFingers(){

    }

    @Override
    public void checkPredecessor() {

    }

    private void startSocketListener(){
        SocketNodeListener socketNodeListener = new SocketNodeListener(this);
        Executors.newCachedThreadPool().submit(socketNodeListener);
    }

    private int getNodeId() {
        return nodeId;
    }

}
