package node;

public interface NodeInterface {

    public void stabilize();

    public void notify(Node n);

    public void fixFingers();

    public void checkPredecessor();

    public Node findSuccessor(int id);

    public Node closestPrecedingNode(int id);
}
