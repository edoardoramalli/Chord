package node;

import java.io.IOException;
import java.io.Serializable;

public interface NodeInterface extends Serializable {

    void notify(NodeInterface node) throws IOException;

    void checkPredecessor();

    NodeInterface findSuccessor(Long id) throws IOException;

    NodeInterface closestPrecedingNode(Long id);

    NodeInterface getPredecessor();

    NodeInterface getSuccessor();

    String getIpAddress();

    Long getNodeId();

    void close() throws IOException;

}
