package network.message;

import java.io.IOException;
import java.io.Serializable;

public class GetIpAddressRequest implements Message, Serializable {
    private Long lockId;

    public GetIpAddressRequest(Long lockId) {
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getLockId() {
        return lockId;
    }
}
