package network.message;

import java.io.IOException;
import java.io.Serializable;

public class GetIpAddressResponse implements Message, Serializable {
    private String ipAddress;
    private Long lockId;

    public GetIpAddressResponse(String ipAddress, Long lockId) {
        this.ipAddress = ipAddress;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getLockId() {
        return lockId;
    }
}
