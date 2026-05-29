package auctionserver.base;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    protected String id;
    //LocalDateTime createdAt;
    
    public Entity(String id) {
        this.id = id;
        //this.createdAt = createdAt;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
}
