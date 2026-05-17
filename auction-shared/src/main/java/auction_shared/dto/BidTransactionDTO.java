package auction_shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransactionDTO implements Serializable {
    private AuctionDTO auction;
    private UserDTO bidder;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransactionDTO(AuctionDTO auction, UserDTO bidder, double bidAmount, LocalDateTime bidTime) {
        this.auction = auction;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
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

    public LocalDateTime getBidTime() {
        return bidTime;
    }
}
