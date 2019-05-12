package controller;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.out;

public class Statistics {
    private static Statistics statisticsInstance;
    private Map<Long, NodeInfo> nodeMap;

    private Statistics(){
        this.nodeMap = new HashMap<>();
    }

    public static Statistics getStatistics(){
        if (statisticsInstance == null)
            statisticsInstance = new Statistics();
        return statisticsInstance;
    }

    synchronized void newConnection(Long nodeId){
        out.println("Connessione arrivata: " + nodeId);
        LocalTime connectionTime = LocalTime.now();
        nodeMap.put(nodeId, new NodeInfo(connectionTime));
        if (nodeMap.size() == 1)
            nodeMap.get(nodeId).setStabilityTime(connectionTime);
    }
}
