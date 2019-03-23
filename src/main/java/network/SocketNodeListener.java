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
                out.println("ACCETTATO");
                out.println("MIO:");
                out.println("Ip: " + socketIn.getLocalAddress().getHostAddress());
                out.println("Porta: " + socketIn.getLocalPort());
                out.println("ALTRO:");
                out.println("Ip: " + socketIn.getInetAddress().getHostAddress());
                out.println("Porta: " + socketIn.getPort());
                SocketNode socketNode = new SocketNode(node, socketIn);
                node.createConnection(socketNode);
                executors.submit(socketNode);
                if (false)
                    break;
            }
        }
        catch (IOException e) {
            out.println("ERRORE SL");
            e.printStackTrace();
        }
    }
}
