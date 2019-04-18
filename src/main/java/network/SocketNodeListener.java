package network;

import node.Node;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.out;

public class SocketNodeListener implements Runnable, Serializable {
    private int socketPort;
    private transient Node node;

    public SocketNodeListener(Node node, int socketPort){
        this.node = node;
        this.socketPort = socketPort;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(socketPort)) {
            ExecutorService executors = Executors.newCachedThreadPool();
            while (true) {
                Socket socketIn = serverSocket.accept();
                out.println("----- Benvenuto nella Repubblica di Firenze -----");
                SocketNode socketNode = new SocketNode(node, socketIn);
                executors.submit(socketNode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
