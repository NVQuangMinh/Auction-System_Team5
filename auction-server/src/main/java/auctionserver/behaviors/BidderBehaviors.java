package auctionserver.behaviors;

import auctionserver.behaviors.profile.BidderProfile;
import auctionserver.core.AuctionManager;
import auctionserver.dao.DefaultDAOProvider;
import auctionserver.entities.Auction;
import auctionserver.entities.BidTransaction;
import auctionserver.entities.User;
import auctionserver.exception.BidException;
import auctionserver.service.BidService;

public class BidderBehaviors implements BidderProfile {

    private final User currentUser;
    private final BidService bidService;

    public BidderBehaviors(User currentUser) {
        this.currentUser = currentUser;
        this.bidService = new BidService(new DefaultDAOProvider());
    }

    @Override
    public void placeBid(String itemId, double amount) {
        Auction auction = AuctionManager.getInstance().getRoom(itemId);
        if (auction == null) {
            return;
        }
        BidTransaction transaction = new BidTransaction(auction, currentUser, amount);
        try {
            bidService.processAndSaveBid(auction, transaction);
        } catch (BidException e) {
            // Log rồi swallow — caller không có context để xử lý
            // Đây là design decision: behaviors không throw vì không có response channel
        }
    }
}
