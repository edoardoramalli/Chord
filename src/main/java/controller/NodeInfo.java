package controller;

import java.time.LocalTime;

/**
 * The aim of this class is to keep track of the statistics of each connected Node to the Controller.
 * This object is used in the map of the controller as value associated to the NodeId as key of the map.
 * Statistics:
 * - Stability of the Node
 * - Starting time of the Lookup
 * - Starting time of the InsertKey
 * - Starting time of the FindKey
 */
class NodeInfo {
    private boolean stable;
    private LocalTime startTimeLookup;
    private LocalTime startTimeInsertKey;
    private LocalTime startTimeFindKey;

    NodeInfo() { this.stable = false; }

    void setStability(boolean stability) {
        this.stable= stability;
    }

    boolean getStability() {return this.stable;}

    LocalTime getStartTimeLookup() {return this.startTimeLookup;}

    void setStartTimeLookup(LocalTime startTimeLookup){
        this.startTimeLookup = startTimeLookup;
    }

    LocalTime getStartTimeInsertKey() { return startTimeInsertKey; }

    void setStartTimeInsertKey(LocalTime startTimeInsertKey) {
        this.startTimeInsertKey = startTimeInsertKey;
    }

    LocalTime getStartTimeFindKey() { return startTimeFindKey; }

    void setStartTimeFindKey(LocalTime startTimeFindKey) { this.startTimeFindKey = startTimeFindKey; }
}