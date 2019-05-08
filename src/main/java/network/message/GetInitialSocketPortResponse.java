package network.message;

import java.io.IOException;
import java.io.Serializable;

public class GetInitialSocketPortResponse implements Message, Serializable {
    private int socketPort;
    private Long lockId;

    public GetInitialSocketPortResponse(int socketPort, Long lockId) {
        this.socketPort = socketPort;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public int getSocketPort() {
        return socketPort;
    }

    public Long getLockId() {
        return lockId;
    }
}
