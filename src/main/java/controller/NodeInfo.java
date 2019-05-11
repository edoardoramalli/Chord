package controller;

import java.time.LocalTime;

public class NodeInfo {
    private LocalTime connectionTime;
    private LocalTime stabilityTime;
    private boolean stable;

    public NodeInfo(LocalTime connectionTime) {
        this.connectionTime = connectionTime;
        this.stabilityTime = null;
        this.stable = false;
    }

    public void setStabilityTime(LocalTime stabilityTime) {
        this.stabilityTime = stabilityTime;
    }
}
