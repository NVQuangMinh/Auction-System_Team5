package auction_server.entities;

import java.io.Serializable;
import java.util.UUID;

public class BidTransaction implements Serializable {
    private String id;
    Auction auction;
    User bidder;
    private double bidAmount;

    public BidTransaction(Auction auction, User bidder, double bidAmount) {
        this.id = UUID.randomUUID().toString();
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
    }

    public double getBidAmount() {
        return this.bidAmount;
    }

    public User getBidder() {
        return bidder;
    }

    public Auction getAuction() {
        return auction;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
