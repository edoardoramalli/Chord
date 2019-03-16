package node;

import java.io.IOException;
import java.io.Serializable;

public interface NodeInterface extends Serializable {

    void notify(NodeInterface node) throws IOException;

    void checkPredecessor();

    NodeInterface findSuccessor(Long id) throws IOException;

    NodeInterface closestPrecedingNode(Long id) throws IOException;

    NodeInterface getPredecessor() throws IOException;

    NodeInterface getSuccessor();

    String getIpAddress();

    int getSocketPort();

    Long getNodeId() throws IOException;

    void close() throws IOException;

}
