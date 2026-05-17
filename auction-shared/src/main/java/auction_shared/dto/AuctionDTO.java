package auction_shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionDTO implements Serializable {
    private String auctionId;
    private AuctionStatus status;
    private ItemDTO item;
    private double startingPrice;
    private double buyOutPrice;
    private double tickSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentHighestBid;

    public AuctionDTO(ItemDTO item, AuctionStatus status, double startingPrice, double buyOutPrice,
                      double tickSize, LocalDateTime startTime, LocalDateTime endTime, double currentHighestBid) {
        this.auctionId = item.getId();
        this.status = status;
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }
}
