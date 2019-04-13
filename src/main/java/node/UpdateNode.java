package node;

import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UpdateNode implements Runnable {
    private Node node;

    UpdateNode(Node node){
        this.node = node;
    }

    @Override
    public void run() {
        boolean equal_figer = false;
        boolean equal_list = false;
        while (true){
            try {
                if (node.getPredecessor() != null){
                    //Get Old List Value to be compared at the end
                    ArrayList<Long> oldSuccList = new ArrayList<>();
                    for (NodeInterface  n : node.getSuccessorList()) {
                        oldSuccList.add(n.getNodeId());
                    }

                    node.listStabilize();

                    ArrayList<Long> newSuccList = new ArrayList<>();
                    for (NodeInterface  n : node.getSuccessorList()) {
                        newSuccList.add(n.getNodeId());
                    }

                    equal_list = oldSuccList.equals(newSuccList); //The Order matters

                    oldSuccList.clear();
                    newSuccList.clear();
                }
                // get old Finger Table
                ArrayList<Long> oldFigerTableList = new ArrayList<>();
                for (Map.Entry<Integer, NodeInterface> entry : node.getFingerTable().entrySet()) {
                    oldFigerTableList.add(entry.getValue().getNodeId());
                }

                for (int i = 0; i < node.getDimFingerTable(); i++)
                    node.fixFingers();

                ArrayList<Long> newFigerTableList = new ArrayList<>();
                for (Map.Entry<Integer, NodeInterface> entry : node.getFingerTable().entrySet()) {
                    newFigerTableList.add(entry.getValue().getNodeId());
                }

                equal_figer = oldFigerTableList.equals(newFigerTableList);  //The Order matters

                oldFigerTableList.clear();
                newFigerTableList.clear();

                node.updateStable(equal_list, equal_figer);
            } catch (IOException e) {
                throw new UnexpectedBehaviourException();
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }
}
