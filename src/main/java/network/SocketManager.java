package network;

import exceptions.ConnectionErrorException;
import node.Node;
import node.NodeInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class SocketManager {
    private volatile Node node;
    private volatile Map<Long, NodeInterface> socketList;
    private volatile Map<Long, Integer> socketNumber;

    public SocketManager(Node node) {
        this.node = node;
        this.socketList = new HashMap<>();
        this.socketNumber = new HashMap<>();
    }

    public synchronized NodeInterface createConnection(NodeInterface connectionNode) throws IOException, ConnectionErrorException {
        Long searchedNodeId = connectionNode.getNodeId();
        if (searchedNodeId.equals(node.getNodeId())) { //nel caso in cui ritorno me stesso non ho bisogno di aggiornare il numero di connessioni
            out.println("CREATECONN, RITORNO ME STESSO");
            return node;
        }
        else {
            NodeInterface searchedNode = socketList.get(searchedNodeId);
            if(searchedNode != null) {
                out.println("DALLA LISTA: " + searchedNodeId);
                int n = socketNumber.get(searchedNodeId); //vecchio numero di connessioni
                socketNumber.replace(searchedNodeId, n+1); //faccio replace con nodeId e n+1
                return searchedNode;
            }
            else{
                out.println("NUOVO: " + searchedNodeId);
                NodeCommunicator createdNode;
                try {
                    createdNode = new NodeCommunicator(connectionNode.getIpAddress(), connectionNode.getSocketPort(), node, node.hash(connectionNode.getIpAddress()));
                } catch (ConnectionErrorException e) {
                    throw new ConnectionErrorException();
                }
                socketList.put(searchedNodeId, createdNode);
                socketNumber.put(searchedNodeId, 1); //quando creo un nodo inserisco nella lista <nodeId, 1>
                return createdNode;
            }
        }
    }

    NodeInterface createConnection(SocketNode socketNode, String ipAddress){
        out.println("CREO: " + node.hash(ipAddress));
        NodeInterface createdNode = new NodeCommunicator(socketNode, node, node.hash(ipAddress));
        socketList.put(node.hash(ipAddress), createdNode);
        socketNumber.put(node.hash(ipAddress), 1); //quando creo un nodo inserisco nella lista <nodeId, 1>
        return createdNode;
    }

    public void closeCommunicator(Long nodeId) {
        //eseguo solo se il nodeId da rimuovere non è il mio
        if (!node.getNodeId().equals(nodeId)){ //non posso rimuovere me stesso dalla lista
            Integer n = socketNumber.get(nodeId); //old connection number
            if (n != null){
                if (n == 1) { //removes the connection
                    socketNumber.remove(nodeId);
                    socketList.remove(nodeId);
                } else //decreases the number of connection
                    socketNumber.replace(nodeId, n-1);
                out.println("RIMOSSO: " + nodeId);
            }
        }
    }

    void removeNode(Long disconnectedId){
        node.checkDisconnectedNode(disconnectedId);
        closeCommunicator(disconnectedId);
    }
}
