package auction_shared.dto;

import java.io.Serializable;

public class PlaceBidRequestDTO implements Serializable {
    private Long auctionId;
    private double amount;

    // Constructor, Getters
    public PlaceBidRequestDTO(Long auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    public Long getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
}