package controller;

import org.apache.commons.lang.ObjectUtils;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



import static java.lang.System.out;

public class Statistics {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static Statistics statisticsInstance;
    private LocalTime startTimeStability, endTimeStability;
    private volatile Map<Long, NodeInfo> nodeMap;

    private Statistics(){
        this.nodeMap = new LinkedHashMap<>();
    }

    public static Statistics getStatistics(){
        if (statisticsInstance == null)
            statisticsInstance = new Statistics();
        return statisticsInstance;
    }

    synchronized void newConnection(Long nodeId){
        nodeMap.put(nodeId, new NodeInfo());
        this.startTimeStability = LocalTime.now();
        if (nodeMap.size() == 1)
            updateStable(nodeId, true);
        out.println("Connessione arrivata: " + nodeId + ". Dim: " + nodeMap.size());
    }

    synchronized void disconnectedNode(Long nodeId){
        nodeMap.remove(nodeId);
        this.startTimeStability = LocalTime.now();
        out.println("Disconnessione: " + nodeId + ". Dim: " + nodeMap.size());
    }

    synchronized void updateStable(Long nodeId, boolean stable){
        nodeMap.get(nodeId).setStability(stable);
        if(stable){
            int iter = 0;
            List<NodeInfo> tempList = new ArrayList<>(nodeMap.values());
            boolean tempStability = tempList.get(0).getStability();
            do{
                tempStability = tempStability && tempList.get(iter).getStability();
                iter++;
            }while(tempStability && (iter < tempList.size()));
            if(tempStability){
                this.endTimeStability = LocalTime.now();
                long elapsed = Duration.between(startTimeStability, endTimeStability).toMillis();
                double pass = elapsed / 1000.0;
                out.println(ANSI_GREEN + "Tempo per Stabilizzarsi : " + pass + " sec. - Numero Nodi "
                        + nodeMap.size() + ANSI_RESET);
            }
        }
    }

    synchronized void startLookup(Long nodeId){
        nodeMap.get(nodeId).setStartTimeLookup(LocalTime.now());
    }

    synchronized void endLookup(Long nodeId){
        LocalTime endTime = LocalTime.now();
        LocalTime startTime = nodeMap.get(nodeId).getStartTimeLookup();
        long elapsed = Duration.between(startTime, endTime).toMillis();
        double pass = elapsed / 1000.0;
        out.println(ANSI_CYAN + "Tempo per Lookup : " + pass + " sec. - Numero Nodi "
                + nodeMap.size() + ANSI_RESET);
    }

    synchronized void startInsertKey(Long nodeId){
        nodeMap.get(nodeId).setStartTimeInsertKey(LocalTime.now());
    }

    synchronized void endInsertKey(Long nodeId){
        LocalTime endTime = LocalTime.now();
        LocalTime startTime = nodeMap.get(nodeId).getStartTimeInsertKey();
        long elapsed = Duration.between(startTime, endTime).toMillis();
        double pass = elapsed / 1000.0;
        out.println(ANSI_PURPLE + "Tempo per Insert Key : " + pass + " sec. - Numero Nodi "
                + nodeMap.size() + ANSI_RESET);
    }

    synchronized void startFindKey(Long nodeId){
        nodeMap.get(nodeId).setStartTimeFindKey(LocalTime.now());
    }

    synchronized void endFindKey(Long nodeId){
        LocalTime endTime = LocalTime.now();
        LocalTime startTime = nodeMap.get(nodeId).getStartTimeFindKey();
        long elapsed = Duration.between(startTime, endTime).toMillis();
        double pass = elapsed / 1000.0;
        out.println(ANSI_YELLOW + "Tempo per Find Key : " + pass + " sec. - Numero Nodi "
                + nodeMap.size() + ANSI_RESET);
    }
}
