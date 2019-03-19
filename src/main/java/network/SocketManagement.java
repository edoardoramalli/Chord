package network;

import exceptions.ConnectionErrorException;
import node.NodeInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class SocketManagement {
    private static SocketManagement socketManagementInstance;
    private Map<String, NodeCommunicator> nameSocket;
    private volatile NodeInterface node;

    private SocketManagement(){
        this.nameSocket = new HashMap<>();
    }

    public static SocketManagement getInstance(){
        if (socketManagementInstance == null)
            socketManagementInstance = new SocketManagement();
        return socketManagementInstance;
    }

    public void setNode(NodeInterface node) {
        this.node = node;
    }

    public static void clear(){
        if (socketManagementInstance != null)
            socketManagementInstance = null;
    }

    public NodeInterface createConnection(NodeInterface connectionNode) throws IOException {
        Long nodeId = connectionNode.getNodeId();
        String ipAddress = connectionNode.getIpAddress();
        int socketPort = connectionNode.getSocketPort();
        String key = nodeId.toString() + ipAddress + socketPort;
        NodeCommunicator searchedNode = nameSocket.get(key);
        if(searchedNode != null) {
            out.println("DALLA LISTA");
            return searchedNode;
        }
        else{
            out.println("NUOVO");
            NodeCommunicator createdNode = null;
            try {
                createdNode = new NodeCommunicator(connectionNode.getIpAddress(), connectionNode.getSocketPort(), node);
                nameSocket.put(key, createdNode);
            } catch (ConnectionErrorException e) {
                e.printStackTrace(); //TODO da fare qualcosa
            }
            return createdNode;
        }
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
