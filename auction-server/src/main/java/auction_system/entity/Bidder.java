package auction_system.entity;

public class Bidder extends User {
    public Bidder(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    public void placeBid(Auction auction, double amount) {
        auction.handleNewBid(this, amount);
    }
}