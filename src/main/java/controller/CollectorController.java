package controller;

import controller.message.*;

import java.io.IOException;

public class CollectorController implements ControllerMessageHandler {
    private Collector collector;
    private SocketController socketController;

    public CollectorController(Collector collector, SocketController socketController) {
        this.collector = collector;
        this.socketController = socketController;
    }

    @Override
    public void handle(ConnectedMessage connectedMessage) throws IOException {
        collector.newConnection(connectedMessage.getNodeId());
        socketController.sendMessage(new ReceivedMessage(connectedMessage.getLockId()));
    }

    @Override
    public void handle(StableMessage stableMessage) throws IOException {

    }

    @Override
    public void handle(NotStableMessage notstableMessage) throws IOException {

    }
}
