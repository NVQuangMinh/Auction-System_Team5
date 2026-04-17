package auction_shared.entities;

import java.io.Serializable;

public class BidTransaction implements Serializable {
    Auction auction;
    User bidder;
    double bidAmount;
}
