package node;

import network.NodeCommunicator;

public interface NodeInterface {

    public void stabilize();

    public void notify(Node n);

    public void fixFingers();

    public void checkPredecessor();

    public NodeInterface findSuccessor(long id);

    public NodeInterface closestPrecedingNode(long id);

    public NodeInterface getPredecessor();

    public NodeInterface getSuccessor();

    public  long getNodeId();

}
