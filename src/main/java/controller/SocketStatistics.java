package controller;

import controller.message.StatisticsMessage;
import controller.message.NodeMessage;
import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Executors;

public class SocketStatistics implements Runnable, Serializable {
    private transient StatisticsController controller;
    private transient ObjectInputStream socketInput;
    private transient ObjectOutputStream socketOutput;
    private transient volatile boolean connected = true;

    public SocketStatistics(Statistics statistics, Socket nodeSocket){
        this.controller = new StatisticsController(statistics, this);
        try {
            this.socketInput = new ObjectInputStream(nodeSocket.getInputStream());
            this.socketOutput = new ObjectOutputStream(nodeSocket.getOutputStream());
        } catch (IOException e) {
            this.close();
        }
    }

    /**
     * While the node is connected (connected == true) the method calls the getMessage method to receives the
     * message from the node
     */
    @Override
    public void run() {
        while (connected){
            StatisticsMessage message = getMessage();
            if(!connected)
                break;
            Executors.newCachedThreadPool().execute(() -> {
                try {
                    message.handle(controller);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Receives the message from the node,
     * and when the other node has disconnected calls the nodeDisconnected method of StatisticsController
     * @return the received message
     */
    private StatisticsMessage getMessage() {
        try {
            return (StatisticsMessage) socketInput.readObject();
        } catch (IOException e) {
            connected = false;
            this.close();
            controller.disconnectedNode();
        } catch (ClassNotFoundException e) {
            throw new UnexpectedBehaviourException();
        }
        return null;
    }

    /**
     * Sends the NodeMessage to the node
     * @param message message to send
     * @throws IOException if an I/O error occurs
     */
    synchronized void sendMessage(NodeMessage message) throws IOException {
        socketOutput.reset();
        socketOutput.writeObject(message);
        socketOutput.flush();
    }

    /**
     * Close the socketInputStream and the socketOutputStream
     */
    private void close() {
        try {
            socketInput.close();
            socketOutput.close();
            this.connected = false;
        } catch (IOException e) {
            throw new UnexpectedBehaviourException();
        }
    }
}
