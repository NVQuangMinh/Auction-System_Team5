package auction_shared.Network;


import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private String action;
    private Serializable data;
    public NetworkMessage(String action, Serializable data){
        this.action = action;
        this.data = data;
    }
    public String getAction(){
        return this.action;
    }
    public Serializable getData(){
        return data;
    }
}