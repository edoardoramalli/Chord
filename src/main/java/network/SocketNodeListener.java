package network;

import node.Node;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketNodeListener implements Runnable, Serializable {
    private int socketPort = 8000;
    private transient Node node;

    public SocketNodeListener(Node node){
        this.node = node;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(socketPort)) {
            ExecutorService executors = Executors.newCachedThreadPool();

            while (true) {
                Socket socketIn = serverSocket.accept();
                executors.submit(new SocketNode(node, socketIn));
                if (false)
                    break;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
