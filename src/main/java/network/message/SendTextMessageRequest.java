package network.message;

import java.io.IOException;
import java.io.Serializable;

public class SendTextMessageRequest implements Message, Serializable {
    private Long source;
    private Long dest;
    private String textMessage;
    private Long lockId;

    public SendTextMessageRequest(Long source, Long dest, String textMessage, Long lockId) {
        this.source = source;
        this.dest = dest;
        this.textMessage = textMessage;
        this.lockId = lockId;
    }

    @Override
    public void handle(MessageHandler messageHandler) throws IOException {
        messageHandler.handle(this);
    }

    public Long getSource() {
        return source;
    }

    public Long getDest() {
        return dest;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public Long getLockId() {
        return lockId;
    }
}
