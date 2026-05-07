package auction_server.interfaces;

public interface BidderAction {
    void placeBid(String itemId, double amount);
}
