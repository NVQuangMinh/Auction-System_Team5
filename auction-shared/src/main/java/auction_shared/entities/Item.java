package auction_shared.entities;

import auction_shared.base.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;


public abstract class Item extends Entity implements Serializable {
    protected String itemName;
    protected String description;
    protected User owner;

    public Item(String id, String itemName, String description, User owner) {
        super(id);
        this.itemName = itemName;
        this.description = description;
        this.owner = owner;
    }
    public String getName(){
        return this.itemName;
    }
    public String getDescription(){
        return this.description;
    }
    public User getOwner(){return this.owner;}



}

