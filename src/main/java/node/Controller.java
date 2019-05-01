package node;

import java.net.*;

import java.io.IOException;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Controller extends Thread {
    private ArrayList<String> socketList;
    private HashMap<Socket, String> socketMap;
    private HashMap<String, String> stableNet;
    private Semaphore trafficLight;
    private Semaphore timeSem;
    private Socket socket; //socket di connessione con il client
    private LocalTime starTime, endTime; //TODO non usati si possono togliere?
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
                System.out.println("Nodi Connessi ("+ socketList.size() + ") : " + socketList  + " " + LocalTime.now());
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
                    System.out.println("Nodi Connessi ("+ socketList.size() + ") : " + socketList  + " " + LocalTime.now());
                    trafficLight.release();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "NotStable":
                try {
                    trafficLight.acquire();
                    stableNet.put(nodeId,"False");
                    System.out.println("Stabilità : " + stableNet.toString());
                    trafficLight.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "Stable":
                try {
                    trafficLight.acquire();
                    stableNet.put(nodeId,"True");
                    System.out.println("Stabilità : " + stableNet.toString() + " " + LocalTime.now());
                    Set<String> values = new HashSet<>(stableNet.values());
                    boolean isUnique = values.size() == 1;
                    if (isUnique){
                        timeSem.acquire();
                        c.setEndTime(LocalTime.now());
                        Long elapsed = Duration.between(c.getStartTime(), c.getEndTime()).getSeconds();
                        timeSem.release();
                        System.out.println("Tempo per Stabilizzarsi : " + elapsed + " sec.");
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

    public static void main(String[] args) throws Exception {

        Collector coll = new Collector();

        try (ServerSocket listener = new ServerSocket(59898)) {
            System.out.println("The Controller server is running...");
            while (true) {
                Executors.newCachedThreadPool().submit(new Controller(listener.accept(), coll));
            }
        }
    }

    //TODO questo non possiamo farlo diventare una classe?
    public static class Collector {
        private ArrayList<String> socketList = new ArrayList<>();
        private HashMap<Socket, String> socketMap = new HashMap<>();
        private Semaphore trafficLight = new Semaphore(1);
        private Semaphore timeSem = new Semaphore(1);
        private HashMap<String, String> stableNet = new HashMap<>();
        private LocalTime startTime;
        private LocalTime endTime;


        public ArrayList<String> getSocketList() {
            return socketList;
        }

        public Semaphore getTrafficLight() {
            return trafficLight;
        }

        public HashMap<String, String> getStableNet() {
            return stableNet;
        }

        public HashMap<Socket, String> getSocketMap() {
            return socketMap;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public Semaphore getTimeSem() {
            return timeSem;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }
    }
}
