package node;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class UpdateNode implements Runnable {
    private Node node;

    UpdateNode(Node node){
        this.node = node;
    }

    @Override
    public void run() {
        while (true){
            try {
                if (node.getPredecessor() != null)
                    node.listStabilize();
                for (int i = 0; i < node.getDimFingerTable(); i++)
                    node.fixFingers();
            } catch (IOException e) {
                e.printStackTrace(); //qui poi devo gestire la disconnessione
            }
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if(false)
                break;
        }
    }
}
