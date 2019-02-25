package node;

import java.util.List;

public class Node {
    private String ipAddress;
    private Node successor;
    private Node predecessor;
    private List<Node> fingerTable; //questa può diventare una map tra il socket e il nodo collegato

    public Node(String ipAddress){
        this.ipAddress = ipAddress;
        this.successor = null;
        this.predecessor = null;
        this.fingerTable = null;
    }

    public void create(){
        successor = this;
        predecessor = null;
    }

    public void join (Node n){

    }

    public void stabilize(){

    }

    public void notify(Node n){

    }

    public void fix_fingers(){

    }

    public void check_predecessor(){

    }
}
