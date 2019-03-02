package node;

import network.NodeCommunicator;

import java.io.IOException;

public interface NodeInterface {

    public void stabilize();

    public void notify(Node n);

    public void fixFingers() throws IOException;

    public void checkPredecessor();

    public NodeInterface findSuccessor(long id) throws IOException;

    public NodeInterface closestPrecedingNode(long id);

    public NodeInterface getPredecessor();

    public NodeInterface getSuccessor();

    public  long getNodeId();

}
