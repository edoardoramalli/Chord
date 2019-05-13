package controller;

import controller.message.StatisticsMessage;
import controller.message.NodeMessage;
import exceptions.ConnectionErrorException;
import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * Socket Node-side that deals the sending and receiving of Message to/from Statistics
 */
public class SocketNodeStatistics implements Runnable, Serializable {
    private String ipAddress;
    private int socketPort;
    private transient Socket socketController;
    private transient ObjectOutputStream out;
    private transient ObjectInputStream in;
    private transient NodeStatisticsController controller;
    private volatile boolean connected;

    /**
     * @param ipAddress ipAddress of Statistics
     * @param socketPort socketPort on which Statistics is reachable
     */
    public SocketNodeStatistics(String ipAddress, int socketPort) {
        this.ipAddress = ipAddress;
        this.socketPort = socketPort;
    }

    /**
     * Creates the output/input stream to the Statistics, and also creates the controller
     * responsible to handle the received message
     * @param nodeId nodeId of node that creates the connection
     * @return the created controller
     * @throws ConnectionErrorException if statistics is not reachable at (ipAddress, socketPort)
     * @throws IOException if an I/O error occurs
     */
    public NodeStatisticsController openController(Long nodeId) throws ConnectionErrorException, IOException {
        this.connected = true;
        try {
            socketController = new Socket(ipAddress, socketPort);
        } catch (IOException e) {
            throw new ConnectionErrorException();
        }
        this.out = new ObjectOutputStream(socketController.getOutputStream());
        this.in = new ObjectInputStream(socketController.getInputStream());
        this.controller = new NodeStatisticsController(nodeId, this);
        Executors.newCachedThreadPool().submit(this);
        return controller;
    }

    /**
     * While the connection is open (connected == true) the method calls the getMessage method to receives the
     * message from Statistics
     */
    @Override
    public void run() {
        while (connected){
            NodeMessage message = getMessage();
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
     * Receives the message from Statistics
     * @return the received message
     */
    private NodeMessage getMessage() {
        try {
            return (NodeMessage) in.readObject();
        } catch (IOException e) {
            //TODO andrebbe gestita la disconnessione del controller
            connected = false;
            this.close();
        } catch (ClassNotFoundException e) {
            throw new UnexpectedBehaviourException();
        }
        return null;
    }

    /**
     * Sends the StatisticsMessage to Statistics
     * @param message message to send
     * @throws IOException if an I/O error occurs
     */
    synchronized void sendMessage(StatisticsMessage message) throws IOException {
        out.reset();
        out.writeObject(message);
        out.flush();
    }

    /**
     * Close the socketInputStream and the socketOutputStream
     */
    private void close() {
        try {
            in.close();
            out.close();
            socketController.close();
            this.connected = false;
        } catch (IOException e) {
            throw new UnexpectedBehaviourException();
        }
    }
}