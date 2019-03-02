package network;

import node.NodeInterface;

import static java.lang.System.out;

class MessageHandler {
    private NodeInterface node;

    MessageHandler(NodeInterface node) {
        this.node = node;
    }

    void handle(String message){
        out.println("Arrivato: " + message);
    }
}
