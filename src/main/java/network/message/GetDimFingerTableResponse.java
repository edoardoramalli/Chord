package network.message;

import java.io.IOException;
import java.io.Serializable;

public class GetDimFingerTableResponse implements Message, Serializable {
    private int dimFingerTable;
    private Long lockId;

    public GetDimFingerTableResponse(int dimFingerTable, Long lockId) {
        this.dimFingerTable = dimFingerTable;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public int getDimFingerTable() {
        return dimFingerTable;
    }

    public Long getLockId() {
        return lockId;
    }
}
