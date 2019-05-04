package node;

import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Collector {
    private ArrayList<String> socketList = new ArrayList<>();
    private HashMap<Socket, String> socketMap = new HashMap<>();
    private Semaphore trafficLight = new Semaphore(1);
    private Semaphore timeSem = new Semaphore(1);
    private HashMap<String, String> stableNet = new HashMap<>();
    private LocalTime startTime;
    private LocalTime endTime;

    ArrayList<String> getSocketList() {
        return socketList;
    }

    Semaphore getTrafficLight() {
        return trafficLight;
    }

    HashMap<String, String> getStableNet() {
        return stableNet;
    }

    HashMap<Socket, String> getSocketMap() {
        return socketMap;
    }

    LocalTime getStartTime() {
        return startTime;
    }

    LocalTime getEndTime() {
        return endTime;
    }

    Semaphore getTimeSem() {
        return timeSem;
    }

    void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
