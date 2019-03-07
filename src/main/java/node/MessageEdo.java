package node;

public class MessageEdo {
    private int id;
    private long source;
    private long destination;
    private String payload;

    public MessageEdo (int id, long s, long d, String t){
        this.payload = t;
        this.source = s;
        this.destination = d;
        this.id = id;
        //per ottimizzazioni. Se invio un msg ad uno nodo che non esiste o morto il msg gira all'infinito
        //nell'anello. Come scegliere id?
    }

    public int getId() {
        return id;
    }

    public long getSource() {
        return source;
    }

    public long getDestination() {
        return destination;
    }

    public String getPayload() {
        return payload;
    }
}