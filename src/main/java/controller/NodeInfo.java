package controller;


import java.time.LocalTime;

public class NodeInfo {

    private boolean stable;
    private LocalTime startTimeLookup;
    private LocalTime startTimeInsertKey;
    private LocalTime startTimeFindKey;


    public NodeInfo() {

        this.stable = false;
    }

    public void setStability(boolean stability) {
        this.stable= stability;
    }

    public boolean getStability() {return this.stable;}

    public LocalTime getStartTimeLookup() {return this.startTimeLookup;}

    public void setStartTimeLookup(LocalTime startTimeLookup){
        this.startTimeLookup = startTimeLookup;
    }

    public LocalTime getStartTimeInsertKey() {
        return startTimeInsertKey;
    }

    public void setStartTimeInsertKey(LocalTime startTimeInsertKey) {
        this.startTimeInsertKey = startTimeInsertKey;
    }

    public LocalTime getStartTimeFindKey() {
        return startTimeFindKey;
    }

    public void setStartTimeFindKey(LocalTime startTimeFindKey) {
        this.startTimeFindKey = startTimeFindKey;
    }
}
