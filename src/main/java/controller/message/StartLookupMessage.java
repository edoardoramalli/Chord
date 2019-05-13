package controller.message;

import java.io.IOException;

public class StartLookupMessage implements StatisticsMessage {
    private Long lockId;

    public StartLookupMessage(Long lockId) {
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
