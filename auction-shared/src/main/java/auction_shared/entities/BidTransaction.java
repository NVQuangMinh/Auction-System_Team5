package auction_shared.entities;

import java.io.Serializable;

public class BidTransaction implements Serializable {
    Auction auction;
    User bidder;
    private double bidAmount;
    public BidTransaction(Auction auction, User bidder, double bidAmount){
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
    }
    public double getBidAmount(){
        return this.bidAmount;
    }

    public User getBidder() {
        return bidder;
    }
}
