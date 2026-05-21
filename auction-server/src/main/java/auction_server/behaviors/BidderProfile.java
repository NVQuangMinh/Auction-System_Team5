package auction_server.behaviors;

public interface BidderProfile {
    void placeBid(String itemId, double amount);
}
