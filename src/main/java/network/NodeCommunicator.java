package network;

import exceptions.ConnectionErrorException;
import node.Node;
import node.NodeInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class NodeCommunicator implements NodeInterface, Serializable {
    private transient Socket joinNodeSocket;
    private transient ObjectOutputStream out;
    private transient ObjectInputStream in;
    private transient long nodeId;

    public NodeCommunicator(String ipAddress, int socketPort) throws ConnectionErrorException, IOException {
        try {
            joinNodeSocket = new Socket(ipAddress, socketPort);
        } catch (IOException e) {
            throw new ConnectionErrorException();
        }
        out = new ObjectOutputStream(joinNodeSocket.getOutputStream());
        in = new ObjectInputStream(joinNodeSocket.getInputStream());

    }

    public void close() throws IOException {
        in.close();
        out.close();
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
    public NodeInterface findSuccessor(long id) {
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
