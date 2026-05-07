package auction_server.service;

import auction_server.dao.interfaces.AuctionDAO;
import auction_server.dao.interfaces.BidTransactionDAO;
import auction_server.dao.interfaces.ItemDAO;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;

import java.util.List;

public class AuctionService {

    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private final BidTransactionDAO bidTransactionDAO;

    public AuctionService(AuctionDAO auctionDAO, ItemDAO itemDAO, BidTransactionDAO bidTransactionDAO) {
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    public Auction placeBid(Long auctionId, User user, double bidAmount) throws Exception {
        Auction auction = auctionDAO.findById(auctionId);

        if (auction == null) {
            throw new Exception("Auction not found.");
        }
        if (!"ACTIVE".equals(auction.getStatus())) {
            throw new Exception("Auction is not active.");
        }

        double requiredAmount = auction.getCurrentHighestBid() + auction.getTickSize();
        if (bidAmount < requiredAmount) {
            throw new Exception("Bid amount must be at least " + requiredAmount + " (current price + tick size).");
        }

        auction.getLock().lock();
        try {
            // Re-check condition after acquiring lock to prevent race conditions
            if (bidAmount >= auction.getCurrentHighestBid() + auction.getTickSize()) {
                
                // Kiểm tra xem giá đặt có vượt qua giá mua đứt không
                if (auction.getBuyOutPrice() > 0 && bidAmount >= auction.getBuyOutPrice()) {
                    auction.setCurrentHighestBid(auction.getBuyOutPrice());
                    auction.setStatus("CLOSED");
                } else {
                    auction.setCurrentHighestBid(bidAmount);
                }
                
                BidTransaction transaction = new BidTransaction(null, auction, user, auction.getCurrentHighestBid());

                bidTransactionDAO.save(transaction);
                auctionDAO.update(auction);
                
                return auction;
            } else {
                throw new Exception("Another bid was placed just before yours. Please try again.");
            }
        } finally {
            auction.getLock().unlock();
        }
    }

    public List<Auction> getActiveAuctions() {
        return auctionDAO.findAllActive();
    }

    public List<Auction> getAuctionsByOwner(Long userId) {
        // TODO: Implement this method in AuctionDAO
        // return auctionDAO.findByOwnerId(userId);
        return List.of();
    }
}
