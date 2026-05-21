package auction_server.behaviors;

import auction_server.core.AuctionManager;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;

public class BidderBehaviors implements BidderProfile {

    private final User currentUser;

    public BidderBehaviors(User currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public void placeBid(String itemId, double amount) {
        Auction auction = AuctionManager.getInstance().getRoom(itemId);
        if (auction == null) {
            return;
        }
        BidTransaction transaction = new BidTransaction(auction, currentUser, amount);
        auction.placeBid(transaction);
    }
}
