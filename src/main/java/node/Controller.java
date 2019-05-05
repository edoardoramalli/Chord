package node;

import java.net.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Controller implements Runnable {
    private ArrayList<String> socketList;
    private HashMap<Socket, String> socketMap;
    private HashMap<String, String> stableNet;
    private Semaphore trafficLight;
    private Semaphore timeSem;
    private Socket socket; //socket di connessione con il client
    private LocalTime starTime, endTime; //todo questi si possono togliere?
    private Collector c;

    public Controller(Socket socket, Collector c) {
        this.socket = socket;
        this.c = c;
        this.trafficLight = c.getTrafficLight();
        this.socketList = c.getSocketList();
        this.socketMap = c.getSocketMap();
        this.stableNet = c.getStableNet();
        this.timeSem = c.getTimeSem();
    }

    @Override
    public void run() {
        //System.out.println("Connected: " + socket);
        try {
            Scanner in = new Scanner(socket.getInputStream());
            //PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            while (in.hasNextLine()) {
                //this.parseInput(in.nextLine());
                String a = in.nextLine();
                this.parseInput(a);
                //out.println(in.nextLine().toUpperCase());
                //out.println("EDOOO");
            }
        } catch (Exception e) {
            System.out.println("Error:" + socket);
            System.out.println("Nodi Connessi : " + socketList  + " " + LocalTime.now());
            System.out.println("End Error");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
            //System.out.println("Closed: " + socket);
            String exitNode = socketMap.get(socket);
            try {
                timeSem.acquire();
                c.setStartTime(LocalTime.now());
                timeSem.release();

                trafficLight.acquire();
                socketMap.remove(socket);
                socketList.remove(exitNode);
                stableNet.remove(exitNode);
                //System.out.println("Nodi Connessi ("+ socketList.size() + ") : " + socketList  + " " + LocalTime.now());
                System.out.println("Nodi Connessi ("+ socketList.size() + ") " + LocalTime.now());
                trafficLight.release();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void parseInput(String input) {
        String [] split = input.split("#");
        String nodeId = split[0];
        switch (split[1]) {
            case "Connected":
                try {
                    timeSem.acquire();
                    c.setStartTime(LocalTime.now());
                    timeSem.release();

                    trafficLight.acquire();
                    socketMap.put(socket,nodeId);
                    socketList.add(nodeId);
                    Collections.sort(socketList);
                    //System.out.println("Nodi Connessi ("+ socketList.size() + ") : " + socketList  + " " + LocalTime.now());
                    System.out.println("Nodi Connessi ("+ socketList.size() + ") " + LocalTime.now());
                    trafficLight.release();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "NotStable":
                try {
                    trafficLight.acquire();
                    stableNet.put(nodeId,"False");
                    //System.out.println("Stabilità : " + stableNet.toString());
                    trafficLight.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "Stable":
                try {
                    trafficLight.acquire();
                    stableNet.put(nodeId,"True");
                    //System.out.println("Stabilità : " + stableNet.toString() + " " + LocalTime.now());
                    Set<String> values = new HashSet<>(stableNet.values());
                    boolean isUnique = values.size() == 1;
                    if (isUnique){
                        timeSem.acquire();
                        c.setEndTime(LocalTime.now());
                        Long elapsed = Duration.between(c.getStartTime(), c.getEndTime()).toMillis();
                        timeSem.release();
                        double pass = elapsed / 1000.0;
                        System.out.println("Tempo per Stabilizzarsi : " + pass + " sec.");
                    }
                    trafficLight.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }
}
