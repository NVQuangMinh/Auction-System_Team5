package entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private Item item;
    private double startingPrice;
    private double bidIncrement;
    private double currentHighestBid;
    private Bidder highestBidder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private List<BidTransaction> transactions;

    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Item item, double startingPrice, double bidIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        this.item = item;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        this.currentHighestBid = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "OPEN";
        this.transactions = new ArrayList<>();
    }

    public void start() {
        this.status = "RUNNING";
    }

    public void close() {
        this.status = "FINISHED";
    }

    public void addTransaction(BidTransaction transaction) {
        this.transactions.add(transaction);
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getBidIncrement() { return bidIncrement; }
    public void setBidIncrement(double bidIncrement) { this.bidIncrement = bidIncrement; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public Bidder getHighestBidder() { return highestBidder; }
    public void setHighestBidder(Bidder highestBidder) { this.highestBidder = highestBidder; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<BidTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<BidTransaction> transactions) { this.transactions = transactions; }
}