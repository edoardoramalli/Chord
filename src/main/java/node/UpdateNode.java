package node;

import java.io.IOException;

public class UpdateNode implements Runnable {
    private Node node;

    public UpdateNode(Node node){
        this.node = node;
    }

    @Override
    public void run() {
        while (true){
            try {
                node.stabilize();
                node.fixFingers();
                node.getFingerTable().get(0).notify(node);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                wait(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
