package network;

import exceptions.ConnectionErrorException;
import network.message.MessageHandler;
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
            out.println("CREO CONNESSIONE: RITORNO ME STESSO");
            return node;
        }
        else {
            NodeInterface searchedNode = socketList.get(searchedNodeId);
            if(searchedNode != null) {
                out.println("CONNESSIONE GIA ESISTENTE VERSO: " + searchedNodeId);
                int n = socketNumber.get(searchedNodeId); //vecchio numero di connessioni
                socketNumber.replace(searchedNodeId, n+1); //faccio replace con nodeId e n+1
                return searchedNode;
            }
            else{
                out.println("NUOVA CONNESSIONE VERSO: " + searchedNodeId);
                NodeCommunicator createdNode;
                try {
                    createdNode = new NodeCommunicator(connectionNode.getIpAddress(), connectionNode.getSocketPort(), node, connectionNode.getNodeId());
                } catch (ConnectionErrorException e) {
                    throw new ConnectionErrorException();
                }
                socketList.put(searchedNodeId, createdNode);
                socketNumber.put(searchedNodeId, 1); //quando creo un nodo inserisco nella lista <nodeId, 1>
                return createdNode;
            }
        }
    }

    synchronized void createConnection(SocketNode socketNode, String ipAddress) {
        NodeInterface createdNode = new NodeCommunicator(socketNode, node);
        socketNode.setMessageHandler((MessageHandler) createdNode);
        int port = 0;
        try {
            port = createdNode.getSocketPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        createdNode.setNodeId(node.hash(ipAddress, port));
        if (!createdNode.getNodeId().equals(node.getNodeId())) {
            out.println("CREO CONNESSIONE DA: " + createdNode.getNodeId());
            socketList.put(createdNode.getNodeId(), createdNode);
            socketNumber.put(createdNode.getNodeId(), 1); //quando creo un nodo inserisco nella lista <nodeId, 1>
        }
    }

    public synchronized void closeCommunicator(Long nodeId) {
        //eseguo solo se il nodeId da rimuovere non è il mio
        if (!node.getNodeId().equals(nodeId)){ //non posso rimuovere me stesso dalla lista
            Integer n = socketNumber.get(nodeId); //old connection number
            if (n != null){
                if (n == 1) { //removes the connection
                    try {
                        socketList.get(nodeId).close();
                    } catch (IOException e) {
                        out.println("Code ad Arosio");
                    }
                    socketNumber.remove(nodeId);
                    socketList.remove(nodeId);
                } else //decreases the number of connection
                    socketNumber.replace(nodeId, n-1);
                out.println("RIMOSSO: " + nodeId);
            }
        }
    }

    synchronized void removeNode(Long disconnectedId){
        node.checkDisconnectedNode(disconnectedId);
        socketList.remove(disconnectedId);
        socketNumber.remove(disconnectedId);
    }

    @Override
    public String toString() {
        String string = "SOCKET OPEN\n";
        for (Map.Entry it:
        socketList.entrySet()){
            string = string + "Node id: " + it.getKey() + "\tNumber conn:" + socketNumber.get(it.getKey());
        }
        return string;
    }
}
