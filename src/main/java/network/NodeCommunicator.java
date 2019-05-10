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

import static java.lang.System.out;

public class NodeCommunicator implements NodeInterface, Serializable, MessageHandler {
    private transient Socket joinNodeSocket;
    private transient NodeInterface node; //mio nodo
    private transient Long nodeId; //questo è il nodeId dell'altro
    private transient String ipAddress; //ipAddress dell'altro nodo
    private transient int socketPort; //socketPort dell'altro nodo
    private transient int dimFingerTable;
    private transient SocketNode socketNode;
    private transient volatile HashMap<Long, Object> lockList = new HashMap<>();
    private transient volatile Long lockID = 0L;

    /**
     * is map <lockId, Message> in this way the calling method can access
     * to the return value using the correspondent lockId
     */
    private transient volatile HashMap<Long, Message> messageList;

    /**
     * in milliseconds
     */
    private static final int TIMEOUT = 1000;

    /**
     * Creates the lock object
     * @return lockId corresponding to the created lock object
     */
    private synchronized Long createLock(){
        lockList.put(lockID, new Object());
        lockID = lockID + 1;
        return lockID - 1;
    }

    public NodeCommunicator(String joinIpAddress, int joinSocketPort, NodeInterface node, long nodeId) throws ConnectionErrorException {
        this.node = node;
        this.nodeId = nodeId;
        this.ipAddress = joinIpAddress;
        this.socketPort = joinSocketPort;
        this.messageList = new HashMap<>();
        this.dimFingerTable = node.getDimFingerTable();
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
        this.dimFingerTable = node.getDimFingerTable();
    }

    @Override
    public void close() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)){
            socketNode.sendMessage(new CloseRequest(lockId));
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

    @Override
    public void notify(NodeInterface node) throws TimerExpiredException {
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

    /**
     * {@inheritDoc}
     * Sends a GetInitialSocketPortRequest to the other node, waits on the objects corresponding to the message index
     * and finally retrieve the returned value from the GetInitialSocketPortResponse corresponding to the message index
     * @return {@inheritDoc}
     * @throws TimerExpiredException {@inheritDoc}
     */
    @Override
    public int getInitialSocketPort() throws TimerExpiredException {
        Long lockId = createLock();
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<?> f = service.submit(() -> {
                synchronized (lockList.get(lockId)){
                    try {
                        socketNode.sendMessage(new GetInitialSocketPortRequest(lockId));
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
        GetInitialSocketPortResponse getInitialSocketPortResponse = (GetInitialSocketPortResponse) messageList.get(lockId);
        messageList.remove(lockId);
        return getInitialSocketPortResponse.getSocketPort();
    }

    /**
     * {@inheritDoc}
     * Sends a GetDimFingerTableRequest to the other node, waits on the objects corresponding to the message index
     * and finally retrieve the returned value from the GetDimFingerTableResponse corresponding to the message index
     * @return {@inheritDoc}
     * @throws TimerExpiredException {@inheritDoc}
     */
    @Override
    public int getInitialDimFingerTable() throws TimerExpiredException {
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
    public NodeInterface findSuccessor(Long id) throws TimerExpiredException {
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
    public List<NodeInterface> getSuccessorList() throws TimerExpiredException {
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


    /**
     * Not used in this class
     * @param text Text string passed to the sender for the Controller
     */
    @Override
    public void sendToController(String text) {
        throw new UnexpectedBehaviourException();
    }

    /**
     * {@inheritDoc}
     */
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

    //Not used in this class
    @Override
    public void addKeyToStore(Map.Entry<Long, Object> keyValue) {
        throw new UnexpectedBehaviourException();
    }

    //Not used in this class
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
    public void updateAfterLeave(Long oldNodeID, NodeInterface newNode) throws IOException {
        Long lockId = createLock();
        NodeInterface newNod= new Node(newNode.getIpAddress(), newNode.getSocketPort());
        newNod.setNodeId(newNode.getNodeId());
        socketNode.sendMessage(new LeaveRequest(oldNodeID, newNod, lockId));
    }

    @Override
    public int getDimFingerTable() {
        return dimFingerTable;
    }

    @Override
    public Long getNodeId() {
        return nodeId;
    }

    @Override
    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String getIpAddress(){
        return ipAddress;
    }

    @Override
    public int getSocketPort(){
        return socketPort;
    }

    @Override
    public void setSocketPort(int socketPort) {
        this.socketPort = socketPort;
    }

    /**
     * Not used in this class
     * @return {@inheritDoc}
     */
    @Override
    public SocketManager getSocketManager() {
        throw new UnexpectedBehaviourException();
    }

    //---------> Handling of Messages

    /**
     * {@inheritDoc}
     * Calls findSuccessor method of node, with the parameters taken from findSuccessorRequest message.
     * After sends a FindSuccessorResponse, containing the obtained object, to the requesting node
     * @param findSuccessorRequest the received findSuccessorRequest message
     * @throws IOException
     */
    @Override
    public void handle(FindSuccessorRequest findSuccessorRequest) throws IOException {
        NodeInterface nodeInterface = null;
        try {
            nodeInterface = node.findSuccessor(findSuccessorRequest.getId());
        } catch (TimerExpiredException e) { //TODO gestire eccezione
            e.printStackTrace();
        }
        NodeInterface nodeTemp = new Node(nodeInterface.getIpAddress(), nodeInterface.getSocketPort(), node.getDimFingerTable());
        socketNode.sendMessage(new FindSuccessorResponse(nodeTemp, findSuccessorRequest.getLockId()));
    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the findSuccessorResponse unlocks the method in wait
     * on the object of lockList and put the findSuccessorResponse in messageList
     * @param findSuccessorResponse the received findSuccessorResponse message
     * @throws IOException
     */
    @Override
    public void handle(FindSuccessorResponse findSuccessorResponse) throws IOException {
        synchronized (lockList.get(findSuccessorResponse.getLockId())){
            messageList.put(findSuccessorResponse.getLockId(), findSuccessorResponse);
            lockList.get(findSuccessorResponse.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Calls notify method of node, with the parameters taken from notifyRequest message.
     * After sends a TerminatedMethodMessage to the requesting node
     * @param notifyRequest the received notifyRequest message
     * @throws IOException
     */
    @Override
    public void handle(NotifyRequest notifyRequest) throws IOException {
        try {
            node.notify(notifyRequest.getNode());
        } catch (TimerExpiredException e) {
            throw new UnexpectedBehaviourException();
        }
        socketNode.sendMessage(new TerminatedMethodMessage(notifyRequest.getLockId()));
    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the findSuccessorResponse unlocks the method in wait
     * on the object of lockList
     * @param terminatedMethodMessage the received TerminatedMethodMessage message
     * @throws IOException
     */
    @Override
    public void handle(TerminatedMethodMessage terminatedMethodMessage) throws IOException {
        synchronized (lockList.get(terminatedMethodMessage.getLockId())){
            lockList.get(terminatedMethodMessage.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Sends a TerminatedMethodMessage to the requesting node,
     * after calls the close method of socket node, and close the communication through the caller
     * @param closeRequest the received closeRequest message
     * @throws IOException
     */
    @Override
    public void handle(CloseRequest closeRequest) throws IOException {
        socketNode.sendMessage(new TerminatedMethodMessage(closeRequest.getLockId()));
        socketNode.close();
        node.getSocketManager().closeCommunicator(nodeId);
    }

    /**
     * {@inheritDoc}
     * Calls getPredecessor method of node, after sends a getPredecessorResponse,
     * containing the predecessor (or null if predecessor==null), to the requesting node
     * @param getPredecessorRequest the received getPredecessorRequest message
     * @throws IOException
     */
    @Override
    public void handle(GetPredecessorRequest getPredecessorRequest) throws IOException {
        NodeInterface predecessor;
        try {
            predecessor = node.getPredecessor();
        } catch (TimerExpiredException e) {
            throw new UnexpectedBehaviourException();
        }
        if (predecessor != null)
            socketNode.sendMessage(new GetPredecessorResponse(new Node(predecessor.getIpAddress(), predecessor.getSocketPort(), node.getDimFingerTable()), getPredecessorRequest.getLockId()));
        else
            socketNode.sendMessage(new GetPredecessorResponse(null, getPredecessorRequest.getLockId()));
    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the getPredecessorResponse unlocks the method in wait
     * on the object of lockList and put the getPredecessorResponse in messageList
     * @param getPredecessorResponse the received getPredecessorResponse message
     * @throws IOException
     */
    @Override
    public void handle(GetPredecessorResponse getPredecessorResponse) throws IOException {
        synchronized (lockList.get(getPredecessorResponse.getLockId())){
            messageList.put(getPredecessorResponse.getLockId(), getPredecessorResponse);
            lockList.get(getPredecessorResponse.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Calls getDimFingerTable method of node, after sends a GetDimFingerTableResponse,
     * containing the obtained value, to the requesting node
     * @param getDimFingerTableRequest the received getDimFingerTableRequest message
     * @throws IOException
     */
    @Override
    public void handle(GetDimFingerTableRequest getDimFingerTableRequest) throws IOException {
        socketNode.sendMessage(new GetDimFingerTableResponse(node.getDimFingerTable(), getDimFingerTableRequest.getLockId()));
    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the getDimFingerTableResponse unlocks the method in wait
     * on the object of lockList and put the getDimFingerTableResponse in messageList
     * @param getDimFingerTableResponse the received getDimFingerTableResponse message
     * @throws IOException
     */
    @Override
    public void handle(GetDimFingerTableResponse getDimFingerTableResponse) throws IOException {
        synchronized (lockList.get(getDimFingerTableResponse.getLockId())){
            messageList.put(getDimFingerTableResponse.getLockId(), getDimFingerTableResponse);
            lockList.get(getDimFingerTableResponse.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Calls getSocketPort method of node, after sends a GetInitialSocketPortResponse,
     * containing the obtained value, to the requesting node
     * @param getInitialSocketPortRequest the received getInitialSocketPortRequest message
     * @throws IOException
     */
    @Override
    public void handle(GetInitialSocketPortRequest getInitialSocketPortRequest) throws IOException {
        socketNode.sendMessage(new GetInitialSocketPortResponse(node.getSocketPort(), getInitialSocketPortRequest.getLockId()));
    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the getInitialSocketPortResponse unlocks the method in wait
     * on the object of lockList and put the getInitialSocketPortResponse in messageList
     * @param getInitialSocketPortResponse the received getInitialSocketPortResponse message
     * @throws IOException
     */
    @Override
    public void handle(GetInitialSocketPortResponse getInitialSocketPortResponse) throws IOException {
        synchronized (lockList.get(getInitialSocketPortResponse.getLockId())){
            messageList.put(getInitialSocketPortResponse.getLockId(), getInitialSocketPortResponse);
            lockList.get(getInitialSocketPortResponse.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Calls getSuccessorList method of node, after sends a GetSuccessorListResponse,
     * containing the obtained list, to the requesting node
     * @param getSuccessorListRequest the received getSuccessorListRequest message
     * @throws IOException
     */
    @Override
    public void handle(GetSuccessorListRequest getSuccessorListRequest) throws IOException {
        CopyOnWriteArrayList<NodeInterface> list = new CopyOnWriteArrayList<>();
        try {
            for (NodeInterface nodeInterface : node.getSuccessorList()) {
                list.add(new Node(nodeInterface.getIpAddress(), nodeInterface.getSocketPort(), node.getDimFingerTable()));
            }
        } catch (TimerExpiredException e) {
            throw new UnexpectedBehaviourException();
        }
        socketNode.sendMessage(new GetSuccessorListResponse(list, getSuccessorListRequest.getLockId()));
    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the getSuccessorListResponse unlocks the method in wait
     * on the object of lockList and put the getSuccessorListResponse in messageList
     * @param getSuccessorListResponse the received getSuccessorListResponse message
     * @throws IOException
     */
    @Override
    public void handle(GetSuccessorListResponse getSuccessorListResponse) throws IOException {
        synchronized (lockList.get(getSuccessorListResponse.getLockId())){
            messageList.put(getSuccessorListResponse.getLockId(), getSuccessorListResponse);
            lockList.get(getSuccessorListResponse.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Calls addKeyToStore method of node, with the parameters taken from addKeyRequest message.
     * After sends a AddKeyResponse, containing the obtained object, to the requesting node
     * @param addKeyRequest the received addKeyRequest message
     * @throws IOException
     */
    @Override
    public void handle(AddKeyRequest addKeyRequest) throws IOException {
        node.addKeyToStore(addKeyRequest.getKeyValue());
        socketNode.sendMessage(new AddKeyResponse(new Node(node.getIpAddress(), node.getSocketPort()), addKeyRequest.getLockId()));

    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the addKeyResponse unlocks the method in wait
     * on the object of lockList and put the addKeyResponse in messageList
     * @param addKeyResponse the received addKeyResponse message
     * @throws IOException
     */
    @Override
    public void handle(AddKeyResponse addKeyResponse) throws IOException {
        synchronized (lockList.get(addKeyResponse.getLockId())){
            messageList.put(addKeyResponse.getLockId(), addKeyResponse);
            lockList.get(addKeyResponse.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Calls retrieveKeyFromStore method of node, with the parameters taken from findSuccessorRequest message.
     * After sends a FindKeyResponse, containing the obtained object, to the requesting node
     * @param findKeyRequest the received findKeyRequest message
     * @throws IOException
     */
    @Override
    public void handle(FindKeyRequest findKeyRequest) throws IOException {
        socketNode.sendMessage(new FindKeyResponse(node.retrieveKeyFromStore(findKeyRequest.getKey()), findKeyRequest.getLockId()));
    }

    /**
     * {@inheritDoc}
     * Takes the lockId parameter from the findKeyResponse unlocks the method in wait
     * on the object of lockList and put the findKeyResponse in messageList
     * @param findKeyResponse the received findKeyResponse message
     * @throws IOException
     */
    @Override
    public void handle(FindKeyResponse findKeyResponse) throws IOException {
        synchronized (lockList.get(findKeyResponse.getLockId())){
            messageList.put(findKeyResponse.getLockId(), findKeyResponse);
            lockList.get(findKeyResponse.getLockId()).notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     * Calls updateAfterLeave method of node, with the parameters taken from leaveRequest message.
     * @param leaveRequest the received leaveRequest message
     * @throws IOException
     */
    @Override
    public void handle(LeaveRequest leaveRequest) throws IOException{
        try {
            node.updateAfterLeave(leaveRequest.getNodeId(), leaveRequest.getNewNode());
        } catch (ConnectionErrorException e) {
            throw new UnexpectedBehaviourException();
        }
    }
}