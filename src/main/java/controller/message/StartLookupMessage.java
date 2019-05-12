package controller.message;

import java.io.IOException;

public class StartLookupMessage implements StatisticsMessage {
    private Long nodeId;
    private Long lockId;

    public StartLookupMessage(Long nodeId, Long lockId) {
        this.nodeId = nodeId;
        this.lockId = lockId;
    }

    @Override
    public void handle(StatisticsMessageHandler statisticsMessageHandler) throws IOException {
        statisticsMessageHandler.handle(this);
    }

    public Long getNodeId() {
        return nodeId;
    }

    public Long getLockId() {
        return lockId;
    }
}
