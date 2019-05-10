package network;

import node.Node;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that manages the accepting of incoming connections
 */
public class SocketNodeListener implements Runnable, Serializable {
    private int socketPort;
    private transient Node node;
    private static Boolean active = true;

    /**
     * @param node node in listening (that will be passed as argument for the creation of SocketNode)
     * @param socketPort socketPort to which the node starts accepting incoming connections
     */
    public SocketNodeListener(Node node, int socketPort){
        this.node = node;
        this.socketPort = socketPort;
    }

    /**
     * Creates a ServerSocket on the socketPort that remains in wait of connection,
     * and foreach connection starts a SocketNode on a different thread
     */
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