package auction_shared.dto;

import java.io.Serializable;

public class BidTransactionDTO implements Serializable {
    private AuctionDTO auction;
    private UserDTO bidder;
    private double bidAmount;

    public BidTransactionDTO(AuctionDTO auction, UserDTO bidder, double bidAmount) {
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public UserDTO getBidder() {
        return bidder;
    }

    public AuctionDTO getAuction() {
        return auction;
    }
}
