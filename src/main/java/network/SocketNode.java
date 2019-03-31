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
        try {
            this.messageHandler = (MessageHandler) node.createConnection(this, socketIn.getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            if(false)
                break;
        }
    }

    private Message getMessage() {
        try {
            Message message = (Message) socketInput.readObject();
            err.println(message.getClass());
            return message;
        } catch (EOFException e){
            out.println("Exception chiusura connessione");
            connected = false;
            this.close();
            // TODO potremmo togliere la chiusura doppia da entrambi i lati.
            //  quando uno chiude la connessione da un lato arriva qui
            //  l'eccezione (forse) e lo chiudiamo in questo modo. oppure
            //  si vede dove lancia l'eccezione e si gestisce lì la chisura
        } catch (IOException e) {
            out.println("Entro qui");
            //e.printStackTrace();
            connected = false;
            this.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    synchronized void sendMessage(Message message) throws IOException {
        out.println(message.getClass());
        socketOutput.reset();
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
