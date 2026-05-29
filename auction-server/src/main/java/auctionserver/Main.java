package auctionserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import auctionserver.Network.SocketServer;
import auctionserver.core.AuctionManager;
import auctionserver.core.AuctionScheduler;
import auctionserver.dao.AuctionDAO;
import auctionserver.dao.BidTransactionDAO;
import auctionserver.dao.DAOProvider;
import auctionserver.dao.DefaultDAOProvider;
import auctionserver.entities.Auction;
import auctionserver.entities.BidTransaction;

public class Main {
    public static void main(String[] args) {

        DAOProvider daoProvider = new DefaultDAOProvider();
        AuctionDAO auctionDAO = daoProvider.auctionDAO();
        BidTransactionDAO bidDAO = daoProvider.bidTransactionDAO();

        // Rebuild all auctions from DB into AuctionManager
        // - ACTIVE auctions: added to AuctionManager (receives bids, tracked by scheduler)
        // - ENDED/SOLD auctions: added to AuctionManager (visible to clients, not modified)
        AuctionManager manager = AuctionManager.getInstance();
        List<Auction> activeAuctions = auctionDAO.selectActiveAuctions();

        Map<String, List<BidTransaction>> activeHistories = bidDAO.selectActiveAuctionsBidHistory();

        for (Auction auction : activeAuctions) {
            List<BidTransaction> history = activeHistories.getOrDefault(auction.getAuctionId(), new ArrayList<>());
            
            for (BidTransaction tx : history) {
                tx.setAuction(auction);
            }
            
            auction.setBidHistory(history);
            manager.addRoom(auction);
        }

        System.out.println("[System] Loaded " + activeAuctions.size() + " active auction(s) from database.");

        // Chạy Scheduler với manager đã có data
        AuctionScheduler scheduler = new AuctionScheduler(manager, daoProvider);
        scheduler.start();
        System.out.println("[System] Auction Scheduler has started.");

        System.out.println("[System] Starting Socket Server...");
        new SocketServer().start(8080);
    }
}
