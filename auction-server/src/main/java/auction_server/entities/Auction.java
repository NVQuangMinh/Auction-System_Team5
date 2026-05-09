package auction_server.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Serializable {
    private Item item;
    private double startingPrice;
    private double buyOutPrice;
    private double tickSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentHighestBid;

    // BID HISTORY LIST
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Item item, double startingPrice, double buyOutPrice, double tickSize, LocalDateTime startTime, LocalDateTime endTime) {
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void addTransaction(BidTransaction transaction) {
        bidHistory.add(transaction);
    }

    public double getCurrentHighestBid() {
        if (bidHistory.isEmpty()) {
            return startingPrice;
        } else {
            return currentHighestBid;
        }
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean placeBid(BidTransaction transaction) {
        lock.lock();
        try {
            if (transaction.getBidAmount() > getCurrentHighestBid() && !getItem().getOwner().getUsername().equals(transaction.getBidder().getUsername())) {
                // we also have to deal with the price that exceed the buy out price

                // I guess this shit is gonna be used to build the diagram.
                // oh yeah and this shit is gonna be used to determine who is the winner too.
                addTransaction(transaction);
                setCurrentHighestBid(transaction.getBidAmount()); //this line is good, leave it!
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean buyOut(BidTransaction transaction) {
        lock.lock();
        try {
            if (! transaction.getBidder().getUsername().equals(getItem().getOwner().getUsername())) {
                item.setOwner(transaction.getBidder());
                return true;
            } else {
                return false;
            }
        }
        finally {
            lock.unlock();
        }

    }
}
