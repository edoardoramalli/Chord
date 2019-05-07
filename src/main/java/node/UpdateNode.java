package node;

import exceptions.TimerExpiredException;
import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class that manage the update of node's attributes (like predecessor, successorList, fingerTable),
 * calling periodically the node's methods (stabilize and fixFinger).
 * The class is also responsible to evaluate the stability of the node
 */
public class UpdateNode implements Runnable {
    private Node node;
    private static Boolean active = true;

    /**
     * @param node node to update
     */
    UpdateNode(Node node){
        this.node = node;
    }

    /**
     * Thread responsible to call periodically node.stabilize() and node.fixFinger(),
     * and to evaluate the stability of the node
     */
    @Override
    public void run() {
        boolean stable = false;
        while (active) {
            if (node.getPredecessor() != null) {
                //Get Old List Value to be compared at the end
                ArrayList<Long> oldSuccList = new ArrayList<>();
                for (NodeInterface n : node.getSuccessorList())
                    oldSuccList.add(n.getNodeId());

                try {
                    node.listStabilize();
                } catch (TimerExpiredException ignored) {
                } catch (IOException e) {
                    throw new UnexpectedBehaviourException();
                }

                ArrayList<Long> newSuccList = new ArrayList<>();
                for (NodeInterface n : node.getSuccessorList())
                    newSuccList.add(n.getNodeId());

                stable = oldSuccList.equals(newSuccList); //The Order matters

                oldSuccList.clear();
                newSuccList.clear();

            }

            // get old Finger Table
            ArrayList<Long> oldFingerTableList = new ArrayList<>();
            for (Map.Entry<Integer, NodeInterface> entry : node.getFingerTable().entrySet()) {
                oldFingerTableList.add(entry.getValue().getNodeId());
            }
            for (int i = 0; i < node.getDimFingerTable(); i++) {
                try {
                    node.fixFingers();
                } catch (IOException e) {
                    throw new UnexpectedBehaviourException();
                } catch (TimerExpiredException ignored) {
                }
            }
            ArrayList<Long> newFingerTableList = new ArrayList<>();
            for (Map.Entry<Integer, NodeInterface> entry : node.getFingerTable().entrySet())
                newFingerTableList.add(entry.getValue().getNodeId());
            stable = stable && oldFingerTableList.equals(newFingerTableList);  //The Order matters
            oldFingerTableList.clear();
            newFingerTableList.clear();
            node.updateStable(stable);

            try {
                TimeUnit.MILLISECONDS.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Called to stop the update when the node has disconnected
     */
    static void stopUpdate() {
        active = false;
    }
}
