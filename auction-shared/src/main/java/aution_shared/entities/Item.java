package aution_shared.entities;

import aution_shared.base.Entity;
import java.time.LocalDateTime;


public abstract class Item extends Entity {
    protected String itemName;
    protected String description;
    protected double startingPrice;
    protected double currentMaxPrice;
    protected LocalDateTime endTime;

    public Item(String id, String itemName, String description, double startingPrice, double currentMaxPrice, LocalDateTime endTime) {
        super(id);
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentMaxPrice = currentMaxPrice;
        this.endTime = endTime;
    }

}
