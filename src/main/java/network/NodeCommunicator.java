package network;

import exceptions.ConnectionErrorException;
import network.message.Message;
import network.message.MessageHandler;
import node.Node;
import node.NodeInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import static java.lang.System.out;

public class NodeCommunicator implements NodeInterface, Serializable {
    private transient Socket joinNodeSocket;
    private transient long nodeId;
    private SocketNode socketNode;

    public NodeCommunicator(String ipAddress, int socketPort) throws ConnectionErrorException, IOException {
        try {
            joinNodeSocket = new Socket(ipAddress, socketPort);
        } catch (IOException e) {
            throw new ConnectionErrorException();
        }
        ObjectOutputStream out = new ObjectOutputStream(joinNodeSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(joinNodeSocket.getInputStream());
        this.socketNode = new SocketNode(in, out);
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
    public NodeInterface findSuccessor(long id) throws IOException {
        socketNode.sendMessage(new Message() {
            @Override
            public void handle(MessageHandler messageHandler) throws IOException {

            }
        });
        return null;
    }

    @Override
    public NodeInterface closestPrecedingNode(long id) {
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
}
