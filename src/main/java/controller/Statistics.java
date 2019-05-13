package controller;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



import static java.lang.System.out;

/**
 * Singleton class in charge of the creation of the network controller. It uses a map with NodeId as key and NodeInfo
 * as value. In this way we can collect all the information needed to measure the time elapsed between the start of an
 * operation, like 'insertkey', 'lookup', 'findkey', and their actual end time. In addition the class is responsable to count
 * the number of nodes present in the Chord network and is capable of measuring the convergence time between two
 * successive join or leave of a node to the network itself.
 * All the methods are called by the class 'StatisticsController' that has the corresponding methods that are triggered
 * when a specific type of message is arrived.
 */
public class Statistics {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";

    private static Statistics statisticsInstance;
    private LocalTime startTimeStability;
    private volatile Map<Long, NodeInfo> nodeMap;

    private Statistics(){
        this.nodeMap = new LinkedHashMap<>();
    }

    /**
     * The singleton design pattern is applied
     * @return The singleton object
     */
    public static Statistics getStatistics(){
        if (statisticsInstance == null)
            statisticsInstance = new Statistics();
        return statisticsInstance;
    }

    /**
     * Put in the map the new node of the network. The start time of the convergence is resetted.
     * @param nodeId of the entering new node
     */
    synchronized void newConnection(Long nodeId){
        nodeMap.put(nodeId, new NodeInfo());
        this.startTimeStability = LocalTime.now();
        if (nodeMap.size() == 1)
            updateStable(nodeId, true);
        out.println("Connessione arrivata: " + nodeId + ". Dim: " + nodeMap.size());
    }

    /**
     * The start time of the convergence is resetted. The node with the specific Id is removed from the map.
     * @param nodeId of the node that is exiting for the network
     */
    synchronized void disconnectedNode(Long nodeId){
        nodeMap.remove(nodeId);
        this.startTimeStability = LocalTime.now();
        out.println("Disconnessione: " + nodeId + ". Dim: " + nodeMap.size());
    }

    /**
     * Every time a message of time 'StableMessage' or 'NotStableMessage' is arrived, means that the internal
     * stability status of a node is changed. So the controller is informed and update the corresponding status of
     * the node in the map. If all nodes present in the map are stable, the elapsed time between this event and the
     * start time of the convergence is computed.
     * @param nodeId of the node that has sent this corresponding message
     * @param stable the value of stability carried with the message
     */
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
                LocalTime endTimeStability = LocalTime.now();
                long elapsed = Duration.between(startTimeStability, endTimeStability).toMillis();
                double pass = elapsed / 1000.0;
                out.println(ANSI_GREEN + "Tempo per Stabilizzarsi : " + pass + " sec. - Numero Nodi "
                        + nodeMap.size() + ANSI_RESET);
            }
        }
    }

    /**
     * Setting of the starting time of the lookup function.
     * @param nodeId of the node that has sent this corresponding message
     */
    synchronized void startLookup(Long nodeId){
        nodeMap.get(nodeId).setStartTimeLookup(LocalTime.now());
    }

    /**
     * Is computed the elapsed time between the starting time of the operation, until the confirmation message
     * of that functionality is arrived about a specific node.
     * @param nodeId of the node that has sent this corresponding message
     */
    synchronized void endLookup(Long nodeId){
        LocalTime endTime = LocalTime.now();
        LocalTime startTime = nodeMap.get(nodeId).getStartTimeLookup();
        long elapsed = Duration.between(startTime, endTime).toMillis();
        double pass = elapsed / 1000.0;
        out.println(ANSI_CYAN + "Tempo per Lookup : " + pass + " sec. - Numero Nodi "
                + nodeMap.size() + ANSI_RESET);
    }

    /**
     * Setting of the starting time of the lookup function.
     * @param nodeId of the node that has sent this corresponding message
     */
    synchronized void startInsertKey(Long nodeId){
        nodeMap.get(nodeId).setStartTimeInsertKey(LocalTime.now());
    }

    /**
     * Is computed the elapsed time between the starting time of the operation, until the confirmation message
     * of that functionality is arrived about a specific node.
     * @param nodeId of the node that has sent this corresponding message
     */
    synchronized void endInsertKey(Long nodeId){
        LocalTime endTime = LocalTime.now();
        LocalTime startTime = nodeMap.get(nodeId).getStartTimeInsertKey();
        long elapsed = Duration.between(startTime, endTime).toMillis();
        double pass = elapsed / 1000.0;
        out.println(ANSI_PURPLE + "Tempo per Insert Key : " + pass + " sec. - Numero Nodi "
                + nodeMap.size() + ANSI_RESET);
    }

    /**
     * Setting of the starting time of the lookup function.
     * @param nodeId of the node that has sent this corresponding message
     */
    synchronized void startFindKey(Long nodeId){
        nodeMap.get(nodeId).setStartTimeFindKey(LocalTime.now());
    }

    /**
     * Is computed the elapsed time between the starting time of the operation, until the confirmation message
     * of that functionality is arrived about a specific node.
     * @param nodeId of the node that has sent this corresponding message
     */
    synchronized void endFindKey(Long nodeId){
        LocalTime endTime = LocalTime.now();
        LocalTime startTime = nodeMap.get(nodeId).getStartTimeFindKey();
        long elapsed = Duration.between(startTime, endTime).toMillis();
        double pass = elapsed / 1000.0;
        out.println(ANSI_YELLOW + "Tempo per Find Key : " + pass + " sec. - Numero Nodi "
                + nodeMap.size() + ANSI_RESET);
    }
}
