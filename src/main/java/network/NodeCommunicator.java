package network;

import exceptions.ConnectionErrorException;
import network.message.*;
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
    private transient NodeInterface node; //mio nodo
    private transient long nodeId; //questo è il "mio" nodeId //TODO andrebbe inizializzato da qualche parte
    private SocketNode socketNode;
    private volatile HashMap<Long, Object> lockList = new HashMap<>();
    private volatile Long lockID = 0L;

    private transient volatile Long returnedNodeId;
    private transient volatile NodeInterface returnedNode;
    private transient volatile Integer returnedInt;

    private synchronized Long createLock(){
        lockList.put(lockID, new Object());
        lockID = lockID + 1;
        return lockID - 1;
    }

    public NodeCommunicator(String joinIpAddress, int joinSocketPort, NodeInterface node) throws ConnectionErrorException, IOException {
        this.node = node;
        try {
            joinNodeSocket = new Socket(joinIpAddress, joinSocketPort);
        } catch (IOException e) {
            throw new ConnectionErrorException();
        }
        ObjectOutputStream out = new ObjectOutputStream(joinNodeSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(joinNodeSocket.getInputStream());
        this.socketNode = new SocketNode(in, out, this);
        Executors.newCachedThreadPool().submit(socketNode);
        //inizializzazione valori di ritorno
        nullReturnValue();
    }

    //used by SocketNode, when StartSocketListener accepts a new connection
    NodeCommunicator(SocketNode socketNode, NodeInterface node){
        this.socketNode = socketNode;
        this.node = node;
        //inizializzazione valori di ritorno
        nullReturnValue();
    }

    private void nullReturnValue(){
        this.returnedNodeId = null;
        this.returnedNode = null;
        this.returnedInt = null;
    }

    @Override
    public void close() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            socketNode.sendMessage(new CloseMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        socketNode.close();
        joinNodeSocket.close();
    }

    @Override
    public void notify(NodeInterface node) throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new NotifyRequest(node, lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void checkPredecessor() {

    }

    @Override
    public String getIpAddress (){
        return  node.getIpAddress();
    }

    @Override
    public int getSocketPort() {
        return node.getSocketPort();
    }

    @Override
    public int getDimFingerTable() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new GetDimFingerTableRequest(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        int dimFingerTable = returnedInt;
        returnedInt = null;
        return dimFingerTable;
    }

    @Override
    public NodeInterface findSuccessor(Long id) throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new FindSuccessorRequest(id, lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        NodeInterface nodeR = returnedNode;
        returnedNode = null;
        return nodeR;
    }

    @Override
    public NodeInterface closestPrecedingNode(Long id) {
        return null;
    }

    @Override
    public NodeInterface getPredecessor() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new GetPredecessorRequest(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        NodeInterface nodeR = returnedNode;
        returnedNode = null;
        return nodeR;
    }

    @Override
    public NodeInterface getSuccessor() {
        return null;
    }

    @Override
    public Long getNodeId() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new GetNodeIdRequest(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Long nodeIdR = returnedNodeId;
        returnedNodeId = null;
        return nodeIdR;
    }

    //---------> Handling of Messages

    @Override
    public void handle(FindSuccessorRequest findSuccessorRequest) throws IOException {
        NodeInterface nodeInterface = node.findSuccessor(findSuccessorRequest.getId());
        socketNode.sendMessage(new FindSuccessorResponse(nodeInterface, findSuccessorRequest.getLockId()));
    }

    @Override
    public void handle(FindSuccessorResponse findSuccessorResponse) throws IOException {
        synchronized (lockList.get(findSuccessorResponse.getLockId())){
            while (returnedNode != null){ //questo serve nel caso in cui altri metodi stanno usando returnedNode
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            returnedNode = findSuccessorResponse.getNode();
            lockList.get(findSuccessorResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(NotifyRequest notifyRequest) throws IOException {
        node.notify(notifyRequest.getNode());
        socketNode.sendMessage(new TerminatedMethodMessage(notifyRequest.getLockId()));
    }

    @Override
    public void handle(TerminatedMethodMessage terminatedMethodMessage) throws IOException {
        synchronized (lockList.get(terminatedMethodMessage.getLockId())){
            lockList.get(terminatedMethodMessage.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(CloseMessage closeMessage) throws IOException {
        socketNode.sendMessage(new TerminatedMethodMessage(closeMessage.getLockId()));
        socketNode.close();
    }

    @Override
    public void handle(GetPredecessorRequest getPredecessorRequest) throws IOException {
        socketNode.sendMessage(new GetPredecessorResponse(node.getPredecessor(), getPredecessorRequest.getLockId()));
    }

    @Override
    public void handle(GetPredecessorResponse getPredecessorResponse) throws IOException {
        synchronized (lockList.get(getPredecessorResponse.getLockId())){
            while (returnedNode != null){ //questo serve nel caso in cui altri metodi stanno usando returnedNode
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            returnedNode = getPredecessorResponse.getNode();
            lockList.get(getPredecessorResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(GetNodeIdRequest getNodeIdRequest) throws IOException {
        socketNode.sendMessage(new GetNodeIdResponse(node.getNodeId(), getNodeIdRequest.getLockId()));
    }

    @Override
    public void handle(GetNodeIdResponse getNodeIdResponse) throws IOException {
        synchronized (lockList.get(getNodeIdResponse.getLockId())){
            while (returnedNodeId != null){ //questo serve nel caso in cui altri metodi stanno usando returnedNode
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            returnedNodeId = getNodeIdResponse.getNodeId();
            lockList.get(getNodeIdResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(GetDimFingerTableRequest getDimFingerTableRequest) throws IOException {
        socketNode.sendMessage(new GetDimFingerTableResponse(node.getDimFingerTable(), getDimFingerTableRequest.getLockId()));
    }

    @Override
    public void handle(GetDimFingerTableResponse getDimFingerTableResponse) throws IOException {
        synchronized (lockList.get(getDimFingerTableResponse.getLockId())){
            while (returnedInt != null){
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            returnedInt = getDimFingerTableResponse.getDimFingerTable();
            lockList.get(getDimFingerTableResponse.getLockId()).notifyAll();
        }
    }
}
