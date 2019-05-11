package controller;

import controller.message.ControllerMessage;
import controller.message.NodeMessage;
import exceptions.ConnectionErrorException;
import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Executors;

public class SocketNodeCommunicator implements Runnable, Serializable {
    private String ipAddress;
    private int socketPort;
    private transient Socket socketController;
    private transient ObjectOutputStream out;
    private transient ObjectInputStream in;
    private transient SocketNodeController controller;
    private volatile boolean connected;

    public SocketNodeCommunicator(String ipAddress, int socketPort) {
        this.ipAddress = ipAddress;
        this.socketPort = socketPort;
    }

    public SocketNodeController openController(Long nodeId) throws ConnectionErrorException, IOException {
        this.connected = true;
        try {
            socketController = new Socket(ipAddress, socketPort);
        } catch (IOException e) {
            throw new ConnectionErrorException();
        }
        this.out = new ObjectOutputStream(socketController.getOutputStream());
        this.in = new ObjectInputStream(socketController.getInputStream());
        this.controller = new SocketNodeController(nodeId, this);
        Executors.newCachedThreadPool().submit(this);
        return controller;
    }

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
     * Receives the message from the node,
     * and when the other node has disconnected calls the nodeDisconnected method of NodeCommunicator
     * @return the received message
     */
    private NodeMessage getMessage() {
        try {
            return (NodeMessage) in.readObject();
        } catch (IOException e) {
            connected = false;
            this.close();
        } catch (ClassNotFoundException e) {
            throw new UnexpectedBehaviourException();
        }
        return null;
    }

    /**
     * Sends the message to the node
     * @param message message to send
     * @throws IOException if an I/O error occurs
     */
    synchronized void sendMessage(ControllerMessage message) throws IOException {
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