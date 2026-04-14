package service;

import auction_client.entity.Auction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    private static volatile AuctionManager instance;
    private final Map<Long, Auction> activeAuctions;

    private AuctionManager() {
        this.activeAuctions = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    public Auction getAuction(Long auctionId) {
        return activeAuctions.get(auctionId);
    }

    public void removeAuction(Long auctionId) {
        activeAuctions.remove(auctionId);
    }
}
