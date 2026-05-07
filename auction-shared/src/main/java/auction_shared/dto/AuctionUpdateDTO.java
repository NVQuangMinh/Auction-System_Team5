package auction_shared.dto;

import java.io.Serializable;

public class AuctionUpdateDTO implements Serializable {
    private Long auctionId;
    private double newHighestBid;
    private String bidderUsername;

    // Constructor, Getters
    public AuctionUpdateDTO(Long auctionId, double newHighestBid, String bidderUsername) {
        this.auctionId = auctionId;
        this.newHighestBid = newHighestBid;
        this.bidderUsername = bidderUsername;
    }

    public Long getAuctionId() { return auctionId; }
    public double getNewHighestBid() { return newHighestBid; }
    public String getBidderUsername() { return bidderUsername; }
}