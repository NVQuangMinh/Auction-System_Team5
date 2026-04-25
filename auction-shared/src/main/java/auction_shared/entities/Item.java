package auction_shared.entities;

import auction_shared.base.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;


public abstract class Item extends Entity implements Serializable {
    protected String itemName;
    protected String description;
    protected double startingPrice;
    protected double buyOutPrice;
    protected double productTickRate;
    protected LocalDateTime endTime;
    protected User owner;

    public Item(String id, String itemName, String description, double startingPrice, double buyOutPrice, double productTickRate, LocalDateTime endTime, User owner) {
        super(id);
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.productTickRate = productTickRate; // Added productTickRate
        this.endTime = endTime;
        this.owner = owner;
    }
    public String getName(){
        return this.itemName;
    }
    public String getProductDescription(){
        return this.description;
    }

    public double getBuyOutPrice() {
        return this.buyOutPrice;
    }
    public double getStartingPrice() {
        return this.startingPrice;
    }
    public double getProductTickRate() {
        return this.productTickRate;
    }
}
