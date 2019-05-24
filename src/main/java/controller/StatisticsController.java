package controller;

import controller.message.*;

import java.io.IOException;

/**
 * StatisticsController is Controller-side and performs the handle of the incoming messages and lunch
 * the corresponding method of the controller.
 */
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
        statistics.updateStable(nodeId, true);
        socketStatistics.sendMessage(new ReceivedMessage(stableMessage.getLockId()));
    }

    @Override
    public void handle(NotStableMessage notstableMessage) throws IOException {
        statistics.updateStable(nodeId, false);
        socketStatistics.sendMessage(new ReceivedMessage(notstableMessage.getLockId()));
    }

    @Override
    public void handle(StartLookupMessage startLookupMessage) throws IOException {
        statistics.startLookup(nodeId);
        socketStatistics.sendMessage(new ReceivedMessage(startLookupMessage.getLockId()));
    }

    @Override
    public void handle(EndOfLookupMessage endOfLookupMessage) throws IOException {
        statistics.endLookup(nodeId);
        socketStatistics.sendMessage(new ReceivedMessage(endOfLookupMessage.getLockId()));
    }

    @Override
    public void handle(StartInsertKeyMessage startInsertKeyMessage) throws IOException {
        statistics.startInsertKey(nodeId);
        socketStatistics.sendMessage(new ReceivedMessage(startInsertKeyMessage.getLockId()));
    }

    @Override
    public void handle(EndInsertKeyMessage endInsertKeyMessage) throws IOException {
        statistics.endInsertKey(nodeId);
        socketStatistics.sendMessage(new ReceivedMessage(endInsertKeyMessage.getLockId()));
    }

    @Override
    public void handle(StartFindKeyMessage startFindKeyMessage) throws IOException {
        statistics.startFindKey(nodeId);
        socketStatistics.sendMessage(new ReceivedMessage(startFindKeyMessage.getLockId()));
    }

    @Override
    public void handle(EndFindKeyMessage endFindKeyMessage) throws IOException {
        statistics.endFindKey(nodeId);
        socketStatistics.sendMessage(new ReceivedMessage(endFindKeyMessage.getLockId()));
    }

    void disconnectedNode(){
        statistics.disconnectedNode(nodeId);
    }
}
