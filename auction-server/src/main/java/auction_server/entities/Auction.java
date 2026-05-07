package auction_server.entities;

import auction_server.base.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private final Item item;
    private final double startingPrice;
    private final double buyOutPrice; // Mua đứt
    private final double tickSize;    // Bước giá tối thiểu
    private double currentHighestBid;
    private String status; // e.g., "ACTIVE", "CLOSED", "CANCELLED"
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(String id, Item item, double startingPrice, double buyOutPrice, double tickSize, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.currentHighestBid = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "ACTIVE";
    }

    public void addTransaction(BidTransaction tx) {
        synchronized (bidHistory) {
            bidHistory.add(tx);
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }

    // Getters
    public Item getItem() {
        return item;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getBuyOutPrice() {
        return buyOutPrice;
    }

    public double getTickSize() {
        return tickSize;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
