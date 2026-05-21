package auction_shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {
    private String auctionId;
    private AuctionStatus status;
    private ItemDTO item;
    private ItemType type;
    private double startingPrice;
    private double buyOutPrice;
    private double tickSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean antiSniping;
    private String winnerId;
    private double currentHighestBid;

    public AuctionDTO(ItemDTO item, ItemType type, AuctionStatus status, double startingPrice, double buyOutPrice,
                      double tickSize, LocalDateTime startTime, LocalDateTime endTime,
                      boolean antiSniping, String winnerId, double currentHighestBid) {
        this.auctionId = item.getId();
        this.status = status;
        this.item = item;
        this.type = type;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.startTime = startTime;
        this.endTime = endTime;
        this.antiSniping = antiSniping;
        this.winnerId = winnerId;
        this.currentHighestBid = currentHighestBid;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public ItemDTO getItem() {
        return item;
    }

    public ItemType getType() {
        return type;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getBuyOutPrice() {
        return buyOutPrice;
    }

    public double getTickSize() {
        return tickSize;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isAntiSniping() {
        return antiSniping;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }
}
