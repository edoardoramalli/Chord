package network;

import exceptions.ConnectionErrorException;
import node.Node;
import node.NodeInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class SocketManager {
    //private static SocketManager socketManagerInstance;
    private Map<Long, NodeInterface> socketList; //map tra nodeId e nodeCommunicator
    private Map<Long, Integer> socketNumber; //map tra nodeId e numero di connessioni verso quel nodo
    private volatile Node node;

    public SocketManager(Node node){
        this.node = node;
        this.socketList = new HashMap<>();
        this.socketNumber = new HashMap<>();
    }

    /*
    private SocketManager(){
        this.socketList = new HashMap<>();
        this.socketNumber = new HashMap<>();
    }

    public static SocketManager getInstance(){
        if (socketManagerInstance == null)
            socketManagerInstance = new SocketManager();
        return socketManagerInstance;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public static void clear(){
        if (socketManagerInstance != null)
            socketManagerInstance = null;
    }
*/

    public NodeInterface createConnection(NodeInterface connectionNode) throws IOException, ConnectionErrorException {
        Long searchedNodeId = connectionNode.getNodeId();
        if (searchedNodeId.equals(node.getHostId())) { //se devo aprire una connessione verso me stesso, ritorno me stesso
            out.println("CREATECONN, RITORNO ME STESSO");
            return node;
        }
        else {
            NodeInterface searchedNode = socketList.get(searchedNodeId);
            if(searchedNode != null) { //se il nodo è già presente nella lista ritorno lui
                out.println("DALLA LISTA: " + searchedNodeId);
                return searchedNode;
            }
            else{//se il nodo non è presente lo creo, lo metto nella lista e lo ritorno
                out.println("NUOVO: " + searchedNodeId);
                NodeCommunicator createdNode;
                try {
                    createdNode = new NodeCommunicator(connectionNode.getIpAddress(), connectionNode.getSocketPort(), (NodeInterface) this, node.hash(connectionNode.getIpAddress()));
                } catch (ConnectionErrorException e) {
                    throw new ConnectionErrorException();
                }
                socketList.put(searchedNodeId, createdNode);
                return createdNode;
            }
        }
    }

    //called by startNodeListener, used to create first connection
    NodeInterface createConnection(SocketNode socketNode, String ipAddress) throws IOException {
        out.println("CREO: " + node.hash(ipAddress));
        NodeInterface createdNode = new NodeCommunicator(socketNode, (NodeInterface) this, node.hash(ipAddress));
        socketList.put(node.hash(ipAddress), createdNode);
        return createdNode;
    }

    void closeCommunicator(Long nodeId) throws IOException {
        socketList.remove(nodeId);
        out.println("RIMOSSO: " + nodeId);
    }



}
