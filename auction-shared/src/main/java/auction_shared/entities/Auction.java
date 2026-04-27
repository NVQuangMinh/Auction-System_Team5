package auction_shared.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements Serializable {
    Item item;
    double startingPrice;
    double buyOutPrice;
    double tickSize;
    LocalDateTime startTime;
    LocalDateTime endTime;
    double currentHighestBid;

    // BID HISTORY LIST
    private List<BidTransaction> bidHistory = new ArrayList<>();

    public Auction(Item item,double startingPrice,double buyOutPrice, double tickSize, LocalDateTime startTime, LocalDateTime endTime) {
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void addTransaction(BidTransaction transaction){
        bidHistory.add(transaction);
    }

    public double getCurrentHighestBid() {
        if (bidHistory.isEmpty()){
            return startingPrice;
        }
        else{
            return currentHighestBid;
        }
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public Item getItem() {
        return item;
    }
}
