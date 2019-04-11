package network.message;


import java.io.IOException;
import java.io.Serializable;


public class GetNumberActiveConnectionResponse implements Message, Serializable {
    private Integer numberOfConnection;
    private Long lockId;

    public GetNumberActiveConnectionResponse(Integer numberOfConnection, Long lockId) {
        this.numberOfConnection = numberOfConnection;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Integer getNumberOfConnetion() {
        return numberOfConnection;
    }

    public Long getLockId() {
        return lockId;
    }
}
