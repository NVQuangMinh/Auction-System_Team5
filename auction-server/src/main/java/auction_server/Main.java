package auction_server;

import java.util.List;

import auction_server.Network.SocketServer;
import auction_server.core.AuctionManager;
import auction_server.core.AuctionScheduler;
import auction_server.dao.AuctionDAO;
import auction_server.dao.BidTransactionDAO;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;

public class Main {
    public static void main(String[] args) {

        AuctionDAO auctionDAO = new AuctionDAO();
        BidTransactionDAO bidDAO = new BidTransactionDAO();

        // Rebuild active auctions từ DB vào AuctionManager
        AuctionManager manager = AuctionManager.getInstance();
        List<Auction> activeAuctions = auctionDAO.selectActiveAuctions();

        for (Auction auction : activeAuctions) {
            manager.addRoom(auction);

            // Rebuild bid history từ DB cho mỗi auction
            List<BidTransaction> history = bidDAO.selectByAuctionId(auction.getAuctionId());
            auction.setBidHistory(history);
        }

        System.out.println("[System] Loaded " + activeAuctions.size() + " active auction(s) from database.");

        // Chạy Scheduler với manager đã có data
        AuctionScheduler scheduler = new AuctionScheduler(manager);
        scheduler.start();
        System.out.println("[System] Auction Scheduler has started.");

        System.out.println("[System] Starting Socket Server...");
        new SocketServer().start(8080);
        System.out.println("Hello World");
    }
}
