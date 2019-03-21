package network;

import node.NodeInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class SocketManager {
    private static SocketManager socketManagerInstance;
    private Map<String, NodeCommunicator> nameSocket;
    private volatile NodeInterface node;

    private SocketManager(){
        this.nameSocket = new HashMap<>();
    }

    public static SocketManager getInstance(){
        if (socketManagerInstance == null)
            socketManagerInstance = new SocketManager();
        return socketManagerInstance;
    }

    public void setNode(NodeInterface node) {
        this.node = node;
    }

    public static void clear(){
        if (socketManagerInstance != null)
            socketManagerInstance = null;
    }

    public NodeInterface createConnection(NodeInterface connectionNode) throws IOException {
        Long nodeId = connectionNode.getNodeId();
        String ipAddress = connectionNode.getIpAddress();
        int socketPort = connectionNode.getSocketPort();
        String key = nodeId.toString() + ipAddress + socketPort;
        NodeCommunicator searchedNode = nameSocket.get(key);
        if(searchedNode != null) {
            out.println("DALLA LISTA: " + key);
            return searchedNode;
        }
        else{
            out.println("NUOVO: " + key);
            NodeCommunicator createdNode = null;
            createdNode = new NodeCommunicator(connectionNode.getIpAddress(), connectionNode.getSocketPort(), node);
            nameSocket.put(key, createdNode);
            return createdNode;
        }
    }

    //called by startNodeListener, used to create first connection
    void addConnection(Long nodeId, String ipAddress, int socketPort, NodeCommunicator node) throws IOException {
        String key = nodeId.toString() + ipAddress + socketPort;
        nameSocket.put(key, node);
        out.println("AGGIUNTO: " + key);
    }

    public void closeCommunicator(NodeInterface node) throws IOException {
        Long nodeId = node.getNodeId();
        String ipAddress = node.getIpAddress();
        int socketPort = node.getSocketPort();
        node.close();
        String key = nodeId.toString() + ipAddress + socketPort;
        nameSocket.remove(key);
    }
}
