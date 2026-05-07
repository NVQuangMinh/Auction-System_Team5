package auction_shared.dto;

import java.io.Serializable;

public class PlaceBidRequestDTO implements Serializable {
    private String auctionId;
    private double amount;

    // Constructor, Getters
    public PlaceBidRequestDTO(String auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    public String getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
}