package network;

import exceptions.UnexpectedBehaviourException;
import node.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Executors;

public class SocketNode implements Runnable, Serializable {
    private Node node;
    private transient ObjectOutputStream socketOutput;
    private transient ObjectInputStream socketInput;
    private MessageHandler messageHandler;

    SocketNode(Node node, Socket socketIn){
        this.node = node;
        this.messageHandler = new MessageHandler(node);
        try {
            this.socketInput = new ObjectInputStream(socketIn.getInputStream());
            this.socketOutput = new ObjectOutputStream(socketIn.getOutputStream());
        } catch (IOException e) {
            this.close();
        }
        //TODO forse qua ci vuole un setSocket per Node
    }

    @Override
    public void run() {
        while (true){
            String message = getMessage();
            Executors.newCachedThreadPool().execute(() -> {
                messageHandler.handle(message);
            });
            if(false)
                break;
        }
    }

    public String getMessage() {
        try {
            return ((String) socketInput.readObject());
        } catch (IOException e) {
            this.close();
        } catch (ClassNotFoundException e) {
            throw new UnexpectedBehaviourException();
        }
        return null;
    }

    void sendMessage(String message) throws IOException {
        socketOutput.writeObject(message);
        socketOutput.flush();
    }

    private void close() {
        try {
            socketInput.close();
            socketOutput.close();
        } catch (IOException e) {
            throw new UnexpectedBehaviourException();
        }
    }
}
