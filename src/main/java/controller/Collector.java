package controller;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class Collector {
    private static Collector collectorInstance;
    private Map<Long, NodeInfo> nodeMap;

    private Collector(){
        this.nodeMap = new HashMap<>();
    }

    public static Collector getCollector(){
        if (collectorInstance == null)
            collectorInstance = new Collector();
        return collectorInstance;
    }

    synchronized void newConnection(Long nodeId){
        out.println("Connessione arrivata: " + nodeId);
        LocalTime connectionTime = LocalTime.now();
        nodeMap.put(nodeId, new NodeInfo(connectionTime));
        if (nodeMap.size() == 1)
            nodeMap.get(nodeId).setStabilityTime(connectionTime);
    }
}
