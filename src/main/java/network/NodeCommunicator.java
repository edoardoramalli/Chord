package network;

import exceptions.ConnectionErrorException;
import network.message.FindSuccessorRequest;
import network.message.FindSuccessorResponse;
import network.message.Message;
import network.message.MessageHandler;
import node.Node;
import node.NodeInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Executors;

import static java.lang.System.out;

public class NodeCommunicator implements NodeInterface, Serializable, MessageHandler {
    private transient Socket joinNodeSocket;
    private transient NodeInterface node;
    private transient long nodeId;
    private SocketNode socketNode;
    private volatile HashMap<Long, Object> lockList = new HashMap<>();
    private volatile Long lockID = 0L;
    private transient volatile NodeInterface returnedNode;

    private synchronized Long createLock(){
        lockList.put(lockID, new Object());
        lockID = lockID + 1;
        return lockID - 1;
    }

    public NodeCommunicator(String ipAddress, int socketPort, NodeInterface node) throws ConnectionErrorException, IOException {
        this.node = node;
        try {
            joinNodeSocket = new Socket(ipAddress, socketPort);
        } catch (IOException e) {
            throw new ConnectionErrorException();
        }
        ObjectOutputStream out = new ObjectOutputStream(joinNodeSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(joinNodeSocket.getInputStream());
        this.socketNode = new SocketNode(in, out, this);
        Executors.newCachedThreadPool().submit(socketNode);
        this.returnedNode = null;
    }

    //used by SocketNode, when StartSocketListener accepts a new connection
    NodeCommunicator(SocketNode socketNode, NodeInterface node){
        this.socketNode = socketNode;
        this.node = node;
    }

    public void close() throws IOException {
        socketNode.sendMessage(new Message() {
            @Override
            public void handle(MessageHandler messageHandler) throws IOException {

            }
        });
        socketNode.close();
        joinNodeSocket.close();
    }
    @Override
    public void stabilize() {

    }

    @Override
    public void notify(Node n) {

    }

    @Override
    public void fixFingers() {

    }

    @Override
    public void checkPredecessor() {

    }

    @Override
    public String getIpAddress (){
        return  "";
    }

    @Override
    public NodeInterface findSuccessor(Long id)  {
        out.println("CHIAMATA FIND");
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            try {
                socketNode.sendMessage(new FindSuccessorRequest(id, lockId)); //ERRORE NELLA SEND
            } catch (IOException e) {
                out.println("ERRORE QUI");
            }
            out.println("INVIATO MESSAGGIO");
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        out.println("RISPOSTA RICEVUTA");
        NodeInterface nodeR = returnedNode;
        returnedNode = null;
        return nodeR;
    }

    @Override
    public NodeInterface closestPrecedingNode(Long id) {
        return null;
    }

    @Override
    public NodeInterface getPredecessor() {
        return null;
    }

    @Override
    public NodeInterface getSuccessor() {
        return null;
    }

    @Override
    public long getNodeId() {
        return this.nodeId;
    }

    //---------> Handling of Messages

    @Override
    public void handle(FindSuccessorRequest findSuccessorRequest) throws IOException {
        out.println("FIND RICEVUTO");
        socketNode.sendMessage(new FindSuccessorResponse(node.findSuccessor(findSuccessorRequest.getId()), findSuccessorRequest.getLockId()));
        out.println("FIND RISPOSTO");
    }

    @Override
    public void handle(FindSuccessorResponse findSuccessorResponse) throws IOException {
        out.println("RESPONSE RICEVUTA");
        synchronized (lockList.get(findSuccessorResponse.getLockId())){
            returnedNode = findSuccessorResponse.getNode();
            lockList.get(findSuccessorResponse.getLockId()).notifyAll();
        }
    }
}
