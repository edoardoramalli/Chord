package controller;

import controller.message.*;

import java.io.IOException;

public class StatisticsController implements StatisticsMessageHandler {
    private Statistics statistics;
    private SocketStatistics socketStatistics;
    private Long nodeId;

    StatisticsController(Statistics statistics, SocketStatistics socketStatistics) {
        this.statistics = statistics;
        this.socketStatistics = socketStatistics;
    }

    @Override
    public void handle(ConnectedMessage connectedMessage) throws IOException {
        this.nodeId = connectedMessage.getNodeId();
        statistics.newConnection(connectedMessage.getNodeId());
        socketStatistics.sendMessage(new ReceivedMessage(connectedMessage.getLockId()));
    }

    @Override
    public void handle(StableMessage stableMessage) throws IOException {

    }

    @Override
    public void handle(NotStableMessage notstableMessage) throws IOException {

    }

    @Override
    public void handle(StartLookupMessage startLookupMessage) throws IOException {

    }

    @Override
    public void handle(EndOfLookupMessage endOfLookupMessage) throws IOException {

    }

    void disconnectedNode(){
        statistics.disconnectedNode(nodeId);
    }
}
