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
                int n = socketNumber.get(searchedNodeId); //vecchio numero di connessioni
                socketNumber.replace(searchedNodeId, n+1); //faccio replace con nodeId e n+1
                out.println("CONNESSIONE GIA ESISTENTE VERSO: " + searchedNodeId + " Numero Conn = " + socketNumber.get(connectionNode.getNodeId()));

                return searchedNode;
            }
            else{
                NodeCommunicator createdNode;
                try {
                    createdNode = new NodeCommunicator(connectionNode.getIpAddress(), connectionNode.getSocketPort(), node, connectionNode.getNodeId());
                } catch (ConnectionErrorException e) {
                    throw new ConnectionErrorException();
                }
                socketList.put(searchedNodeId, createdNode);
                socketNumber.put(searchedNodeId, 1); //quando creo un nodo inserisco nella lista <nodeId, 1>
                out.println("NUOVA CONNESSIONE VERSO: " + searchedNodeId + " Numero Conn = " + socketNumber.get(connectionNode.getNodeId()));
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
                        Integer num = socketList.get(nodeId).getNumberActiveConnection(node.getNodeId());
                        out.println("Numero di Conn. di " + node.getNodeId() + " Verso " + nodeId + " = " + num.toString());
                        if ( num == 0 ){
                            socketList.get(nodeId).close();
                        }
                    } catch (IOException e) {
                        out.println("Code ad Arosio");
                    }
                    socketNumber.remove(nodeId);
                    socketList.remove(nodeId);
                    out.println("Rimossa Connessione Verso : " + nodeId);
                    out.println("DUNQUE CONNESSIONI VERSO : " + nodeId + " SONO "+ socketNumber.get(nodeId));
                } else {//decreases the number of connection
                    socketNumber.replace(nodeId, n-1);
                    out.println("Rimossa 1 Connessione Verso : " + nodeId);
                }
            }
        }
    }

    synchronized void removeNode(Long disconnectedId){
        node.checkDisconnectedNode(disconnectedId);
        socketList.remove(disconnectedId);
        socketNumber.remove(disconnectedId);
    }

    public Map<Long, Integer> getSocketNumber() {
        return socketNumber;
    }

    @Override
    public String toString() {
        String string = "SOCKET OPEN : \n";
        for (Map.Entry it:
        socketList.entrySet()){
            string = string + "- Node id: " + it.getKey() + "\tNumber conn: " + socketNumber.get(it.getKey()) + "\n";
        }
        return string;
    }
}
