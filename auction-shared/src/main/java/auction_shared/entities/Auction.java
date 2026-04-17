package auction_shared.entities;

import java.io.Serializable;

public class Auction implements Serializable {
    Item item;
    double startingPrice;
    double currentHighestBid;
    String status;

    public Auction(Item item, double startingPrice, double currentHighestBid, String status) {
        this.item = item;
        this.startingPrice = startingPrice;
        this.currentHighestBid = currentHighestBid;
        this.status = status;
    }

    public void addTransaction(BidTransaction transaction){}

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public Item getItem() {
        return item;
    }

}
