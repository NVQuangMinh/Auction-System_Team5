package auctionserver.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction implements Serializable {
    private String id;
    Auction auction;
    User bidder;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction(Auction auction, User bidder, double bidAmount) {
        this.id = UUID.randomUUID().toString();
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
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

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}
