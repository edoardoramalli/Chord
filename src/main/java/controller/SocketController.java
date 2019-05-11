package controller;

import controller.message.ControllerMessage;
import exceptions.UnexpectedBehaviourException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Executors;

public class SocketController implements Runnable, Serializable {
    private transient CollectorController collector;
    private transient ObjectInputStream socketInput;
    private transient ObjectOutputStream socketOutput;
    private transient volatile boolean connected = true;

    public SocketController(Collector collector, Socket nodeSocket){
        this.collector = new CollectorController(collector, this);
        try {
            this.socketInput = new ObjectInputStream(nodeSocket.getInputStream());
            this.socketOutput = new ObjectOutputStream(nodeSocket.getOutputStream());
        } catch (IOException e) {
            this.close();
        }
    }

    @Override
    public void run() {
        while (connected){
            ControllerMessage message = getMessage();
            if(!connected)
                break;
            Executors.newCachedThreadPool().execute(() -> {
                try {
                    message.handle(collector);
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
    private ControllerMessage getMessage() {
        try {
            return (ControllerMessage) socketInput.readObject();
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
