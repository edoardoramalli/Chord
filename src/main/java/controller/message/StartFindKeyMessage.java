package controller.message;

import java.io.IOException;

public class StartFindKeyMessage implements StatisticsMessage {
    private Long lockId;

    public StartFindKeyMessage(Long lockId) {
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
