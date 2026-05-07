package auction_server.entities;

import auction_server.base.Entity;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private Auction auction;
    private User bidder;
    private double bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(Long id, Auction auction, User bidder, double bidAmount) {
        super(id);
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    public boolean isValid() {
        // TODO: Implement validation logic.
        // For example, check if the bid amount is valid for the auction.
        return true;
    }

    public Auction getAuction() {
        return auction;
    }

    public User getBidder() {
        return bidder;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
