package controller;

import java.net.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Semaphore;

import static java.lang.System.out;

/**
 * Class for the object OldController. OldController is in charge of keep track the node presents in the Chord Network,
 * It is capable of knowing when a node is entered in the network, and also when it'll exit.
 * OldController receive from each node of the network if it is stable. If the node change its status, it notifies the
 * controller with its new state. In this way we can measure the time between a new insert in the net and the time
 * when all the network will be stable.
 */
public class OldController implements Runnable { //TODO Davide diceva di mettere metodi sincronizzati...
    private ArrayList<String> socketList;
    private HashMap<Socket, String> socketMap;
    private HashMap<String, String> stableNet;
    private Semaphore socketListSem;
    private Semaphore timeSem;
    private Socket socket; //socket di connessione con il client
    private OldCollector c;

    public OldController(Socket socket, OldCollector c) {
        this.socket = socket;
        this.c = c;
        this.socketListSem = c.getSocketListSem();
        this.socketList = c.getSocketList();
        this.socketMap = c.getSocketMap();
        this.stableNet = c.getStableNet();
        this.timeSem = c.getTimeSem();
    }

    /**
     * Override method "run" of the class Runnable. It is used to open a new socket connection to each incoming
     * connection request. In case of a raise exception, means that the socket is not more valid, it is capable of
     * detect the exit of a node from the Chord Network. Semaphore are used to be sure that on the shared data
     * between thread there aren't concurrent writes.
     */
    @Override
    public void run() {
        //System.out.println("Connected: " + socket);
        try {
            Scanner in = new Scanner(socket.getInputStream());
            while (in.hasNextLine()) {
                String a = in.nextLine();
                this.parseInput(a);
            }
        } catch (Exception e) {
            out.println("Error:" + socket);
            out.println("Nodi Connessi : " + socketList  + " " + LocalTime.now());
            out.println("End Error");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                out.println("A node is exited");
            }
            //System.out.println("Closed: " + socket);
            String exitNode = socketMap.get(socket);
            try {
                timeSem.acquire();
                c.setStartTime(LocalTime.now());
                timeSem.release();

                socketListSem.acquire();
                socketMap.remove(socket);
                socketList.remove(exitNode);
                stableNet.remove(exitNode);
                out.println("Nodi Connessi ("+ socketList.size() + ") : " + socketList  + " " + LocalTime.now());
                //out.println("Nodi Connessi ("+ socketList.size() + ") " + LocalTime.now());
                socketListSem.release();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * The Function parse the messages from the nodes of the network. It is used '#' as special character to identify
     * a special message directed to the controller and the node ID that specifies
     * from which node the message comes from.
     * There are three type of messages. Connected msg means that a new node with a specific ID is entered.
     * NotStable means that the node is not stable. Stable the exact opposite.
     * @param input Text Message received from the node
     */
    private void parseInput(String input) {
        String [] split = input.split("#");
        String nodeId = split[0];
        switch (split[1]) {
            case "Connected":
                try {
                    timeSem.acquire();
                    c.setStartTime(LocalTime.now());
                    timeSem.release();

                    socketListSem.acquire();
                    socketMap.put(socket,nodeId);
                    socketList.add(nodeId);
                    Collections.sort(socketList);
                    out.println("Nodi Connessi ("+ socketList.size() + ") : " + socketList  + " " + LocalTime.now());
                    //out.println("Nodi Connessi ("+ socketList.size() + ") " + LocalTime.now());
                    socketListSem.release();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "NotStable":
                try {
                    socketListSem.acquire();
                    stableNet.put(nodeId,"False");
                    //System.out.println("Stabilità : " + stableNet.toString());
                    socketListSem.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "Stable":
                try {
                    socketListSem.acquire();
                    stableNet.put(nodeId,"True");
                    //System.out.println("Stabilità : " + stableNet.toString() + " " + LocalTime.now());
                    Set<String> values = new HashSet<>(stableNet.values());
                    boolean isUnique = values.size() == 1;
                    if (isUnique){
                        timeSem.acquire();
                        c.setEndTime(LocalTime.now());
                        long elapsed = Duration.between(c.getStartTime(), c.getEndTime()).toMillis();
                        timeSem.release();
                        double pass = elapsed / 1000.0;
                        out.println("Tempo per Stabilizzarsi : " + pass + " sec.");
                    }
                    socketListSem.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }
}
