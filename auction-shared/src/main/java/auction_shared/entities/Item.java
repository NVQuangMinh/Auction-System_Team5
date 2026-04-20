package auction_shared.entities;

import auction_shared.base.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;


public abstract class Item extends Entity implements Serializable {
    protected String itemName;
    protected String description;
    protected double startingPrice;
    protected double currentMaxPrice;
    protected LocalDateTime endTime;
    protected User owner;

    public Item(String id, String itemName, String description, double startingPrice, double currentMaxPrice, LocalDateTime endTime, User owner) {
        super(id);
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentMaxPrice = currentMaxPrice;
        this.endTime = endTime;
        this.owner = owner;
    }
    public String getDetail(){
        return null;
    }

}

