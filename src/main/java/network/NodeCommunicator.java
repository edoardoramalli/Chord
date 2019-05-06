package network;

import exceptions.ConnectionErrorException;
import exceptions.TimerExpiredException;
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
import java.util.Map;
import java.util.concurrent.*;

import static java.lang.System.err;
import static java.lang.System.out;

public class NodeCommunicator implements NodeInterface, Serializable, MessageHandler {
    private transient Socket joinNodeSocket;
    private transient NodeInterface node; //mio nodo
    private transient Long nodeId; //questo è il nodeId dell'altro
    private transient String ipAddress; //ipAddress dell'altro nodo
    private transient int socketPort; //socketPort dell'altro nodo
    private transient SocketNode socketNode;
    private transient volatile HashMap<Long, Object> lockList = new HashMap<>();
    private transient volatile Long lockID = 0L;

    //è una map <lockId, Message> in questo modo il metodo chiamante può accedere
    // al suo valore di ritorno tramite il lockId
    private transient volatile HashMap<Long, Message> messageList;

    private static final int TIMEOUT = 1000; //in milliseconds

    private synchronized Long createLock(){
        lockList.put(lockID, new Object());
        lockID = lockID + 1;
        return lockID - 1;
    }

    public NodeCommunicator(String joinIpAddress, int joinSocketPort, NodeInterface node, long id) throws ConnectionErrorException {
        this.node = node;
        this.nodeId = id;
        this.ipAddress = joinIpAddress;
        this.socketPort = joinSocketPort;
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
    NodeCommunicator(SocketNode socketNode, NodeInterface node, String ipAddress){
        this.socketNode = socketNode;
        this.node = node;
        this.messageList = new HashMap<>();
        this.ipAddress = ipAddress;
    }

    @Override
    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void setSocketPort(int socketPort) {
        this.socketPort = socketPort;
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
    public void notify(NodeInterface node) throws IOException, TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new NotifyRequest(node, lockId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto NOTIFY");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getIpAddress(){
        return ipAddress;
    }

    @Override
    public int getInitialSocketPort() throws IOException, TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new GetSocketPortRequest(lockId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto GETSOCKET PORT");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        GetSocketPortResponse getSocketPortResponse = (GetSocketPortResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getSocketPortResponse.getSocketPort();
    }

    @Override
    public int getSocketPort(){
        return socketPort;
    }

    @Override
    public int getDimFingerTable() throws TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new GetDimFingerTableRequest(lockId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto GET DIM FINGER TABLE");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        GetDimFingerTableResponse getDimFingerTableResponse = (GetDimFingerTableResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getDimFingerTableResponse.getDimFingerTable();
    }

    @Override
    public NodeInterface findSuccessor(Long id) throws IOException, TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new FindSuccessorRequest(id, lockId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto FIND SUCCESSOR");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        FindSuccessorResponse findSuccessorResponse = (FindSuccessorResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return findSuccessorResponse.getNode();
    }

    @Override
    public NodeInterface getPredecessor() throws TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new GetPredecessorRequest(lockId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto GET PREDECESSOR");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
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
    public List<NodeInterface> getSuccessorList() throws IOException, TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new GetSuccessorListRequest(lockId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto GET SUCCESSOR LIST");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        GetSuccessorListResponse getSuccessorListResponse = (GetSuccessorListResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getSuccessorListResponse.getSuccessorList();
    }

    //non utilizzato
    @Override
    public void sendToController(String text) {
        throw new UnexpectedBehaviourException();
    }

    @Override
    public void nodeDisconnected() {
        //out.println("Entro qui, disconnesso: " + nodeId);
        node.getSocketManager().removeNode(nodeId);
    }

    @Override
    public NodeInterface addKey(Map.Entry<Long, Object> keyValue) throws TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new AddKeyRequest(keyValue, lockId));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto ADD KEY");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        AddKeyResponse addKeyResponse = (AddKeyResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return addKeyResponse.getNode();
    }

    @Override
    public void addKeyToStore(Map.Entry<Long, Object> keyValue) {
        throw new UnexpectedBehaviourException(); //non utilizzato?
    }

    //non usìtilizzato
    @Override
    public Object retrieveKeyFromStore(Long key) {
        throw new UnexpectedBehaviourException();
    }

    @Override
    public Object findKey(Long key) throws TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new FindKeyRequest(lockId, key));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        lockList.get(lockId).wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            f.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            out.println("Timer scaduto FIND KEY");
            throw new TimerExpiredException();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        FindKeyResponse findKeyResponse = (FindKeyResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return findKeyResponse.getValue();
    }

    //TODO vedere se mettere timer
    @Override
    public void updateAfterLeave(Long oldNodeID, NodeInterface newNode) throws IOException, ConnectionErrorException {
        Long lockId = createLock();

        NodeInterface newNod= new Node(newNode.getIpAddress(), newNode.getSocketPort());
        newNod.setNodeId(newNode.getNodeId());
        socketNode.sendMessage(new LeaveRequest(oldNodeID, newNod, lockId));

    }

    //---------> Handling of Messages

    @Override
    public void handle(FindSuccessorRequest findSuccessorRequest) throws IOException {
        NodeInterface nodeInterface = null;
        try {
            nodeInterface = node.findSuccessor(findSuccessorRequest.getId());
        } catch (TimerExpiredException e) { //TODO gestire eccezione
            e.printStackTrace();
        }
        NodeInterface nodeTemp = null;
        try {
            nodeTemp = new Node(nodeInterface.getIpAddress(),
                    nodeInterface.getSocketPort(), node.getDimFingerTable());
        } catch (TimerExpiredException e) { //TODO gestire eccezione
            e.printStackTrace();
        }
        socketNode.sendMessage(new FindSuccessorResponse(nodeTemp, findSuccessorRequest.getLockId()));
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
        try {
            node.notify(notifyRequest.getNode());
        } catch (TimerExpiredException e) {
            throw new UnexpectedBehaviourException();
        }
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
        NodeInterface predecessor;
        try {
            predecessor = node.getPredecessor();
        } catch (TimerExpiredException e) {
            throw new UnexpectedBehaviourException();
        }
        if (predecessor != null) {
            try {
                socketNode.sendMessage(new GetPredecessorResponse(new Node(predecessor.getIpAddress(), predecessor.getSocketPort(), node.getDimFingerTable()), getPredecessorRequest.getLockId()));
            } catch (TimerExpiredException e) {
                e.printStackTrace();
            }
        }
        else
            socketNode.sendMessage(new GetPredecessorResponse(null, getPredecessorRequest.getLockId()));
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
        try {
            socketNode.sendMessage(new GetDimFingerTableResponse(node.getDimFingerTable(), getDimFingerTableRequest.getLockId()));
        } catch (TimerExpiredException e) {
            throw new UnexpectedBehaviourException();
        }
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

    @Override
    public void handle(GetSuccessorListRequest getSuccessorListRequest) throws IOException {
        CopyOnWriteArrayList<NodeInterface> list = new CopyOnWriteArrayList<>();
        try {
            for (NodeInterface nodeInterface :
                    node.getSuccessorList()) {
                list.add(new Node(nodeInterface.getIpAddress(), nodeInterface.getSocketPort(), node.getDimFingerTable()));
            }
        } catch (TimerExpiredException e) {
            throw new UnexpectedBehaviourException();
        }
        socketNode.sendMessage(new GetSuccessorListResponse(list, getSuccessorListRequest.getLockId()));
    }

    @Override
    public void handle(GetSuccessorListResponse getSuccessorListResponse) throws IOException {
        synchronized (lockList.get(getSuccessorListResponse.getLockId())){
            messageList.put(getSuccessorListResponse.getLockId(), getSuccessorListResponse);
            lockList.get(getSuccessorListResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(AddKeyRequest addKeyRequest) throws IOException {
        node.addKeyToStore(addKeyRequest.getKeyValue());
        socketNode.sendMessage(new AddKeyResponse(new Node(node.getIpAddress(), node.getSocketPort()), addKeyRequest.getLockId()));

    }

    @Override
    public void handle(AddKeyResponse addKeyResponse) throws IOException {
        synchronized (lockList.get(addKeyResponse.getLockId())){
            messageList.put(addKeyResponse.getLockId(), addKeyResponse);
            lockList.get(addKeyResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(FindKeyRequest findKeyRequest) throws IOException {
        Object value = node.retrieveKeyFromStore(findKeyRequest.getKey());
        socketNode.sendMessage(new FindKeyResponse(findKeyRequest.getLockId(), value));
    }

    @Override
    public void handle(FindKeyResponse findKeyResponse) throws IOException {
        synchronized (lockList.get(findKeyResponse.getLockId())){
            messageList.put(findKeyResponse.getLockId(), findKeyResponse);
            lockList.get(findKeyResponse.getLockId()).notifyAll();
        }
    }

    @Override
    public void handle(LeaveRequest leaveRequest) throws IOException{
        try {
            node.updateAfterLeave(leaveRequest.getNodeId(), leaveRequest.getNewNode());
        } catch (ConnectionErrorException e) {
            throw new UnexpectedBehaviourException();
        }
    }
}