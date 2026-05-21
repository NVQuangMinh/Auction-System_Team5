package auction_server.behaviors;

import auction_server.core.AuctionManager;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;

public class BidderBehaviors implements BidderProfile {

    @Override
    public void placeBid(String itemId, double amount) {
        Auction auction = AuctionManager.getInstance().getRoom(itemId);
        if (auction == null) {
            return;
        }
        BidTransaction transaction = new BidTransaction(auction, new User(null, null, null), amount);
        auction.placeBid(transaction);
    }
}
