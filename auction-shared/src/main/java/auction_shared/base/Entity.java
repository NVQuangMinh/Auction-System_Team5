package auction_shared.base;


import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Entity implements Serializable {
    protected String id;
    //LocalDateTime createdAt;
    public Entity(String id){
        this.id = id;
        //this.createdAt = createdAt;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

