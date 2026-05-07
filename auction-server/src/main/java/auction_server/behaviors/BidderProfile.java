package auction_server.behaviors;

import auction_server.entities.Auction;

public class BidderProfile {
    private double itemBought;

    public BidderProfile() {
        this.itemBought = 0;
    }

    public void placeBid(Auction auction, double amount) {
        // TODO: Implement the logic for placing a bid.
        // This might involve calling a method on the Auction object
        // and handling the response.
    }

    public double getItemBought() {
        return itemBought;
    }
}
