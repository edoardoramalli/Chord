package network;

import exceptions.UnexpectedBehaviourException;
import network.message.Message;
import network.message.MessageHandler;
import node.NodeInterface;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;

import static java.lang.System.err;
import static java.lang.System.out;

public class SocketNode implements Runnable, Serializable {
    private transient ObjectOutputStream socketOutput;
    private transient ObjectInputStream socketInput;
    private transient MessageHandler messageHandler;
    private transient volatile boolean connected;

    SocketNode(NodeInterface node, Socket socketIn){
        try {
            this.socketInput = new ObjectInputStream(socketIn.getInputStream());
            this.socketOutput = new ObjectOutputStream(socketIn.getOutputStream());
        } catch (IOException e) {
            this.close();
        }
        this.connected = true;
        this.messageHandler = (MessageHandler) node.getSocketManager().createConnection(this, socketIn.getInetAddress().getHostAddress());
    }

    SocketNode(ObjectInputStream socketInput, ObjectOutputStream socketOutput, MessageHandler messageHandler){
        this.messageHandler = messageHandler;
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
                    message.handle(messageHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private Message getMessage() {
        try {
            /*Message message = (Message) socketInput.readObject();
            err.println(message);
            return message;*/
            return (Message) socketInput.readObject();
        } catch (IOException e) {
            out.println("Entro qui");
            //e.printStackTrace();
            connected = false;
            messageHandler.nodeDisconnected();
            this.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    synchronized void sendMessage(Message message) throws IOException {
        socketOutput.reset();
        socketOutput.writeObject(message);
        socketOutput.flush();
        //out.println(message);
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
