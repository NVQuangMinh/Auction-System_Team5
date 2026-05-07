package auction_server.entities;

import auction_server.base.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private Item item;
    private double startingPrice;
    private double currentHighestBid;
    private String status; // e.g., "ACTIVE", "CLOSED", "CANCELLED"
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Long id, Item item, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startingPrice = startingPrice;
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

    // Getters and Setters
    public Item getItem() {
        return item;
    }

    public double getStartingPrice() {
        return startingPrice;
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
