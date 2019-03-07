package network;

import exceptions.UnexpectedBehaviourException;
import network.message.Message;
import network.message.MessageHandler;
import node.NodeInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Executors;

public class SocketNode implements Runnable, Serializable {
    private NodeInterface node;
    private transient ObjectOutputStream socketOutput;
    private transient ObjectInputStream socketInput;
    private transient MessageHandler messageHandler; //
    private transient volatile boolean connected;

    SocketNode(NodeInterface node, Socket socketIn){
        this.node = node;
        //this.messageHandler = new MessageHandler(node);
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
            Message message = getMessage();
            if(!connected)
                break;
            Executors.newCachedThreadPool().execute(() -> {
                try {
                    message.handle(new MessageHandler() {
                        @Override
                        public int hashCode() {
                            return super.hashCode();
                        }
                    });
                } catch (IOException e) {
                    throw new UnexpectedBehaviourException();
                }
            });
            if(false)
                break;
        }
    }

    private Message getMessage() {
        try {
            return (Message) socketInput.readObject();
        } catch (IOException e) {
            connected = false;
            this.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    void sendMessage(Message message) throws IOException {
        socketOutput.writeObject(message);
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
