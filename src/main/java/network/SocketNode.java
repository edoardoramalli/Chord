package network;

import exceptions.UnexpectedBehaviourException;
import network.message.Message;
import network.message.MessageHandler;
import node.NodeInterface;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * Class that deals with the sending and receiving of Messages to/from the other node
 */
public class SocketNode implements Runnable, Serializable {
    private transient ObjectOutputStream socketOutput;
    private transient ObjectInputStream socketInput;
    private transient volatile MessageHandler messageHandler;
    private transient volatile boolean connected;
    private final boolean connectionIn;

    /**
     * Constructor called from NodeCommunicator's constructor when we want to create a connection towards
     * another node (so for outgoing connections)
     *
     * @param socketInput    socketInputStream created by NodeCommunicator
     * @param socketOutput   socketOutputStream created by NodeCommunicator
     * @param messageHandler NodeCommunicator instance that handles the received messages
     */
    SocketNode(ObjectInputStream socketInput, ObjectOutputStream socketOutput, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.socketInput = socketInput;
        this.socketOutput = socketOutput;
        this.connected = true;
        this.connectionIn = false;
    }

    /**
     * Constructor called from SocketNodeListener (so for incoming connections)
     *
     * @param node     node at which the connection arrives
     * @param socketIn socket of incoming connection, from which we create the input and output stream
     */
    SocketNode(NodeInterface node, Socket socketIn) {
        this.connectionIn = true;
        try {
            this.socketInput = new ObjectInputStream(socketIn.getInputStream());
            this.socketOutput = new ObjectOutputStream(socketIn.getOutputStream());
        } catch (IOException e) {
            this.close();
        }
        this.connected = true;
        Executors.newCachedThreadPool().execute(() ->
                node.getSocketManager().createConnection(this, socketIn.getInetAddress().getHostAddress()));
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * While the node is connected (connected == true) the method calls the getMessage method to receives the
     * message from the other node
     */
    @Override
    public void run() {
        while (connected) {
            Message message = getMessage();
            if (!connected)
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

    /**
     * Receives the message from the other node,
     * and when the other node has disconnected calls the nodeDisconnected method of NodeCommunicator
     *
     * @return the received message
     */
    private Message getMessage() {
        try {
            return (Message) socketInput.readObject();
        } catch (IOException e) {
            connected = false;
            if (!connectionIn)
                messageHandler.nodeDisconnected();
            this.close();
        } catch (ClassNotFoundException e) {
            throw new UnexpectedBehaviourException();
        }
        return null;
    }

    /**
     * Sends the message to the other node
     *
     * @param message message to send
     * @throws IOException if an I/O error occurs
     */
    synchronized void sendMessage(Message message) throws IOException {
        socketOutput.reset();
        socketOutput.writeObject(message);
        socketOutput.flush();
    }

    /**
     * Close the socketInputStream and the socketOutputStream
     */
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
