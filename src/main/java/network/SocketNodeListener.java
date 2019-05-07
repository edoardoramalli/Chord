package network;

import node.Node;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketNodeListener implements Runnable, Serializable {
    private int socketPort;
    private transient Node node;
    private static Boolean active = true;

    public SocketNodeListener(Node node, int socketPort){
        this.node = node;
        this.socketPort = socketPort;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(socketPort)) {
            ExecutorService executors = Executors.newCachedThreadPool();
            while (active) {
                Socket socketIn = serverSocket.accept();
                //out.println("----- Benvenuto nella Repubblica di Firenze -----");
                SocketNode socketNode = new SocketNode(node, socketIn);
                executors.submit(socketNode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called to stop the listening when the node has disconnected
     */
    public static void stopListening() {
        active = false;
    }
}
