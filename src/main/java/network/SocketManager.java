package network;

import exceptions.ConnectionErrorException;
import node.NodeInterface;

import java.io.IOException;
import java.net.Socket;
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

    public NodeInterface createConnection(NodeInterface connectionNode) throws IOException, ConnectionErrorException {
        String ipAddress = connectionNode.getIpAddress();
        int socketPort = connectionNode.getSocketPort();
        String key = ipAddress + socketPort;
        NodeCommunicator searchedNode = nameSocket.get(key);
        if(searchedNode != null) {
            out.println("DALLA LISTA: " + key);
            return searchedNode;
        }
        else{
            out.println("NUOVO: " + key);
            NodeCommunicator createdNode = new NodeCommunicator(connectionNode.getIpAddress(), connectionNode.getSocketPort(), node);
            nameSocket.put(key, createdNode);
            return createdNode;
        }
    }

    //called by startNodeListener, used to create first connection
    NodeInterface createConnection(SocketNode socketNode, NodeInterface node, Socket socketIn) throws IOException {
        return null;
    }

    public void closeCommunicator(NodeInterface node) throws IOException {
        String ipAddress = node.getIpAddress();
        int socketPort = node.getSocketPort();
        node.close();
        String key = ipAddress + socketPort;
        nameSocket.remove(key);
    }
}
