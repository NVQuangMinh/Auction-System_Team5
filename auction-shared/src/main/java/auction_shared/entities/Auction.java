package auction_shared.entities;

import java.io.Serializable;

public class Auction implements Serializable {
    Item item;
    double startingPrice;
    double currentHighestBid;
    String status;
    public void addTransaction(BidTransaction transaction){}
}
