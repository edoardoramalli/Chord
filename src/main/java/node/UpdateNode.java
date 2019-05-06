package node;

import exceptions.TimerExpiredException;
import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UpdateNode implements Runnable {
    private Node node;
    private static Boolean bool=true;

    UpdateNode(Node node){
        this.node = node;
    }

    public static void setUpdate(boolean b) {
        bool=b;
    }

    @Override
    public void run() {
        boolean equalFinger = false;
        boolean equalList = false;
        while (bool) {
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

                equalList = oldSuccList.equals(newSuccList); //The Order matters

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
            equalFinger = oldFingerTableList.equals(newFingerTableList);  //The Order matters
            oldFingerTableList.clear();
            newFingerTableList.clear();
            node.updateStable(equalList, equalFinger);

            try {
                TimeUnit.MILLISECONDS.sleep(1200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
