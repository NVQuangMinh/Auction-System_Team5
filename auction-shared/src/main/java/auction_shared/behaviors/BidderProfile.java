package auction_shared.behaviors;

import auction_shared.interfaces.BidderAction;

import java.io.Serializable;


public class BidderProfile implements BidderAction, Serializable {
    public void placeBid(String itemId, double amount) {}
}
