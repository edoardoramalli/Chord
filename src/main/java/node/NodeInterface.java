package node;

import java.io.IOException;
import java.io.Serializable;

public interface NodeInterface extends Serializable {

    public void stabilize() throws IOException;

    public void notify(NodeInterface node) throws IOException;

    public void fixFingers() throws IOException;

    public void checkPredecessor();

    public NodeInterface findSuccessor(Long id) throws IOException;

    public NodeInterface closestPrecedingNode(Long id);

    public NodeInterface getPredecessor();

    public NodeInterface getSuccessor();

    public String getIpAddress();

    public Long getNodeId();

    public void close() throws IOException;

}
