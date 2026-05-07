package auction_shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {
    private ItemDTO item;
    private double startingPrice;
    private double buyOutPrice;
    private double tickSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentHighestBid;

    public AuctionDTO(ItemDTO item, double startingPrice, double buyOutPrice, double tickSize, 
                      LocalDateTime startTime, LocalDateTime endTime, double currentHighestBid) {
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentHighestBid = currentHighestBid;
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
