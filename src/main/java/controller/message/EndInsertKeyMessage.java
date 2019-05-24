package controller.message;

import java.io.IOException;

public class EndInsertKeyMessage implements StatisticsMessage {
    private Long lockId;

    public EndInsertKeyMessage(Long lockId) {
        this.lockId = lockId;
    }

    @Override
    public void handle(StatisticsMessageHandler statisticsMessageHandler) throws IOException {
        statisticsMessageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }
}