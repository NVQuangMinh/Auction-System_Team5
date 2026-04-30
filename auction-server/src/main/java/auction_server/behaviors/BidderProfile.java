package auction_server.behaviors;

import auction_server.interfaces.BidderAction;

import java.io.Serializable;

public class BidderProfile implements BidderAction, Serializable {
    public void placeBid(String itemId, double amount) {}
}
