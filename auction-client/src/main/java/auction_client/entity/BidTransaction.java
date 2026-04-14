package auction_client.entity;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private Auction auction;
    private Bidder bidder;
    private double bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(Auction auction, Bidder bidder, double bidAmount, LocalDateTime timestamp) {
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    public boolean isValid() {
        double requiredMinimum = auction.getCurrentHighestBid() + auction.getBidIncrement();
        if (bidAmount < requiredMinimum) {
            return false;
        }

        if (auction.getItem().getSeller().getUsername().equals(bidder.getUsername())) {
            return false;
        }

        if (auction.getHighestBidder() != null && auction.getHighestBidder().getUsername().equals(bidder.getUsername())) {
            return false;
        }

        return true;
    }

    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }

    public Bidder getBidder() { return bidder; }
    public void setBidder(Bidder bidder) { this.bidder = bidder; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}