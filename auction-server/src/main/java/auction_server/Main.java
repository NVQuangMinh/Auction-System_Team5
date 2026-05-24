package auction_server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import auction_server.Network.SocketServer;
import auction_server.core.AuctionManager;
import auction_server.core.AuctionScheduler;
import auction_server.dao.AuctionDAO;
import auction_server.dao.BidTransactionDAO;
import auction_server.dao.DAOProvider;
import auction_server.dao.DefaultDAOProvider;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;

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
