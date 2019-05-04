package node;

import exceptions.TimerExpiredException;
import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UpdateNode implements Runnable {
    private Node node;

    UpdateNode(Node node){
        this.node = node;
    }

    @Override
    public void run() {
        boolean equalFinger = false;
        boolean equalList = false;
        while (true) {
            System.out.println("Loop");

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
            System.out.println("DIm " + node.getDimFingerTable());
            for (int i = 0; i < node.getDimFingerTable(); i++) {
                System.out.println("Fing");
                try {
                    node.fixFingers();
                } catch (IOException e) {
                    throw new UnexpectedBehaviourException();
                }
            }
            System.out.println("QUI1");
            ArrayList<Long> newFingerTableList = new ArrayList<>();
            for (Map.Entry<Integer, NodeInterface> entry : node.getFingerTable().entrySet())
                newFingerTableList.add(entry.getValue().getNodeId());
            System.out.println("QUI2");
            equalFinger = oldFingerTableList.equals(newFingerTableList);  //The Order matters
            System.out.println("QUI3");
            oldFingerTableList.clear();
            newFingerTableList.clear();
            System.out.println("QUI4");
            node.updateStable(equalList, equalFinger);

            try {
                System.out.println("Dormo");
                TimeUnit.MILLISECONDS.sleep(1200);
                System.out.println("Dormooooooo");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
