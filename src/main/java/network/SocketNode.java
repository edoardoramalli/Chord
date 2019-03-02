package network;

import exceptions.UnexpectedBehaviourException;
import node.NodeInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Executors;

import static java.lang.System.out;

public class SocketNode implements Runnable, Serializable {
    private NodeInterface node;
    private transient ObjectOutputStream socketOutput;
    private transient ObjectInputStream socketInput;
    private transient MessageHandler messageHandler;
    private transient volatile boolean connected;

    SocketNode(NodeInterface node, Socket socketIn){
        this.node = node;
        this.messageHandler = new MessageHandler(node);
        try {
            this.socketInput = new ObjectInputStream(socketIn.getInputStream());
            this.socketOutput = new ObjectOutputStream(socketIn.getOutputStream());
        } catch (IOException e) {
            this.close();
        }
        this.connected = true;
    }

    public SocketNode(ObjectInputStream socketInput, ObjectOutputStream socketOutput){
        this.socketInput = socketInput;
        this.socketOutput = socketOutput;
        this.connected = true;
    }

    @Override
    public void run() {
        while (connected){
            out.println("IN ATTESA");
            String message = getMessage();
            out.println("DOPO GET");
            if(!connected)
                break;
            Executors.newCachedThreadPool().execute(() -> {
                out.println("ARRIVATO");
                messageHandler.handle(message);
            });
            if(false)
                break;
        }
    }

    private String getMessage() {
        try {
            return socketInput.readUTF();
        } catch (IOException e) {
            connected = false;
            this.close();
        }
        return null;
    }

    void sendMessage(String message) throws IOException {
        socketOutput.writeUTF(message);
        out.println("INVIATO:" + message);
        socketOutput.flush();
    }

    void close() {
        try {
            socketInput.close();
            socketOutput.close();
            this.connected = false;
        } catch (IOException e) {
            throw new UnexpectedBehaviourException();
        }
    }
}
