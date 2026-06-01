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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
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

        log.info("[Hệ thống] đã tải {} phiên đấu giá đang hoạt động từ cơ sở dữ liệu.",  activeAuctions.size());


        // Chạy Scheduler với manager đã có data
        AuctionScheduler scheduler = new AuctionScheduler(manager, daoProvider);
        scheduler.start();
        log.info("[Hệ thống] Auction Scheduler đã bắt đầu.");

        log.info("[Hệ thống] Khởi động Socket Server...");
        new SocketServer().start(8080);
    }
}
