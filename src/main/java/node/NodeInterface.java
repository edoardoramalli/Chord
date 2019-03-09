package node;

import network.NodeCommunicator;

import java.io.IOException;
import java.io.Serializable;

public interface NodeInterface extends Serializable {

    public void stabilize();

    public void notify(Node n);

    public void fixFingers() throws IOException;

    public void checkPredecessor();

    public NodeInterface findSuccessor(Long id) throws IOException;

    public NodeInterface closestPrecedingNode(Long id);

    public NodeInterface getPredecessor();

    public NodeInterface getSuccessor();

    public String getIpAddress();

    public  long getNodeId();

}
