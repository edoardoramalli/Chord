package network;

import exceptions.ConnectionErrorException;
import exceptions.UnexpectedBehaviourException;
import network.message.*;
import node.Node;
import node.NodeInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import static java.lang.System.err;

public class NodeCommunicator implements NodeInterface, Serializable, MessageHandler {
    private transient Socket joinNodeSocket;
    private transient NodeInterface node; //mio nodo
    private transient long nodeId; //questo è il nodeId dell'altro
    private transient SocketNode socketNode;
    private transient volatile HashMap<Long, Object> lockList = new HashMap<>();
    private transient volatile Long lockID = 0L;

    //è una map <lockId, Message> in questo modo il metodo chiamante può accedere
    // al suo valore di ritorno tramite il lockId
    private transient volatile HashMap<Long, Message> messageList;

    private synchronized Long createLock(){
        lockList.put(lockID, new Object());
        lockID = lockID + 1;
        return lockID - 1;
    }

    public NodeCommunicator(String joinIpAddress, int joinSocketPort, NodeInterface node, long id) throws ConnectionErrorException {
        this.node = node;
        this.nodeId = id;
        this.messageList = new HashMap<>();
        try {
            joinNodeSocket = new Socket(joinIpAddress, joinSocketPort);
        } catch (IOException e) {
            throw new ConnectionErrorException();
        }
        ObjectOutputStream out;
        ObjectInputStream in;
        try {
            out = new ObjectOutputStream(joinNodeSocket.getOutputStream());
            in = new ObjectInputStream(joinNodeSocket.getInputStream());
        } catch (IOException e) {
            throw new UnexpectedBehaviourException();
        }
        this.socketNode = new SocketNode(in, out, this);
        Executors.newCachedThreadPool().submit(socketNode);
    }

    //used by SocketNode, when StartSocketListener accepts a new connection
    NodeCommunicator(SocketNode socketNode, NodeInterface node, long id){
        this.socketNode = socketNode;
        this.node = node;
        this.nodeId = id;
        this.messageList = new HashMap<>();
    }

    public void close() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new CloseMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        socketNode.close();
        joinNodeSocket.close();
        node.getSocketManager().closeCommunicator(nodeId);
    }

    //non usato
    @Override
    public SocketManager getSocketManager() {
        err.println("ERRORE DENTRO GETSOCKETMANAGER in NODECOMMUNICATOR");
        throw new UnexpectedBehaviourException();
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
    public String getIpAddress() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new GetIpAddressRequest(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        GetIpAddressResponse getIpAddressResponse = (GetIpAddressResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getIpAddressResponse.getIpAddress();
    }

    @Override
    public int getSocketPort() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new GetSocketPortRequest(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        GetSocketPortResponse getSocketPortResponse = (GetSocketPortResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getSocketPortResponse.getSocketPort();
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
        GetDimFingerTableResponse getDimFingerTableResponse = (GetDimFingerTableResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getDimFingerTableResponse.getDimFingerTable();
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
        FindSuccessorResponse findSuccessorResponse = (FindSuccessorResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return findSuccessorResponse.getNode();
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
        GetPredecessorResponse getPredecessorResponse = (GetPredecessorResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getPredecessorResponse.getNode();
    }


    @Override
    public Long getNodeId() {
        return nodeId;
    }

    @Override
    public List<NodeInterface> getSuccessorList() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new GetSuccessorListRequest(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        GetSuccessorListResponse getSuccessorListResponse = (GetSuccessorListResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getSuccessorListResponse.getSuccessorList();
    }

    @Override
    public void nodeDisconnected() {
        node.getSocketManager().removeNode(nodeId);
    }

    //---------> Handling of Messages

    @Override
    public void handle(FindSuccessorRequest findSuccessorRequest) throws IOException {
        NodeInterface nodeInterface = node.findSuccessor(findSuccessorRequest.getId());
        socketNode.sendMessage(new FindSuccessorResponse(new Node(nodeInterface.getIpAddress(), nodeInterface.getSocketPort()), findSuccessorRequest.getLockId()));
    }

    @Override
    public void handle(FindSuccessorResponse findSuccessorResponse) throws IOException {
        synchronized (lockList.get(findSuccessorResponse.getLockId())){
            messageList.put(findSuccessorResponse.getLockId(), findSuccessorResponse);
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
        node.getSocketManager().closeCommunicator(nodeId);
    }

    @Override
    public void handle(GetPredecessorRequest getPredecessorRequest) throws IOException {
        NodeInterface pred = node.getPredecessor();
        socketNode.sendMessage(new GetPredecessorResponse(new Node(pred.getIpAddress(), pred.getSocketPort()), getPredecessorRequest.getLockId()));
    }

    @Override
    public void handle(GetPredecessorResponse getPredecessorResponse) throws IOException {
        synchronized (lockList.get(getPredecessorResponse.getLockId())){
            messageList.put(getPredecessorResponse.getLockId(), getPredecessorResponse);
            lockList.get(getPredecessorResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(GetDimFingerTableRequest getDimFingerTableRequest) throws IOException {
        socketNode.sendMessage(new GetDimFingerTableResponse(node.getDimFingerTable(), getDimFingerTableRequest.getLockId()));
    }

    @Override
    public void handle(GetDimFingerTableResponse getDimFingerTableResponse) throws IOException {
        synchronized (lockList.get(getDimFingerTableResponse.getLockId())){
            messageList.put(getDimFingerTableResponse.getLockId(), getDimFingerTableResponse);
            lockList.get(getDimFingerTableResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(GetIpAddressRequest getIpAddressRequest) throws IOException {
        socketNode.sendMessage(new GetIpAddressResponse(node.getIpAddress(), getIpAddressRequest.getLockId()));
    }

    @Override
    public void handle(GetIpAddressResponse getIpAddressResponse) throws IOException {
        synchronized (lockList.get(getIpAddressResponse.getLockId())){
            messageList.put(getIpAddressResponse.getLockId(), getIpAddressResponse);
            lockList.get(getIpAddressResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(GetSocketPortRequest getSocketPortRequest) throws IOException {
        socketNode.sendMessage(new GetSocketPortResponse(node.getSocketPort(), getSocketPortRequest.getLockId()));
    }

    @Override
    public void handle(GetSocketPortResponse getSocketPortResponse) throws IOException {
        synchronized (lockList.get(getSocketPortResponse.getLockId())){
            messageList.put(getSocketPortResponse.getLockId(), getSocketPortResponse);
            lockList.get(getSocketPortResponse.getLockId()).notifyAll();
        }
    }

    //TODO BISOGNA CONTROLLARE SE VA, o se dobbiamo clonare la lista prima di ritornarla
    @Override
    public void handle(GetSuccessorListRequest getSuccessorListRequest) throws IOException {
        socketNode.sendMessage(new GetSuccessorListResponse(node.getSuccessorList(), getSuccessorListRequest.getLockId()));
    }

    @Override
    public void handle(GetSuccessorListResponse getSuccessorListResponse) throws IOException {
        synchronized (lockList.get(getSuccessorListResponse.getLockId())){
            messageList.put(getSuccessorListResponse.getLockId(), getSuccessorListResponse);
            lockList.get(getSuccessorListResponse.getLockId()).notifyAll();
        }
    }
}
