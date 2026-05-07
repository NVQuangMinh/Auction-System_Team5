package auction_shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {
    private Long id;
    private ItemDTO item;
    private double startingPrice;
    private double buyOutPrice; // Mới
    private double tickSize;    // Mới
    private double currentHighestBid;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public AuctionDTO(Long id, ItemDTO item, double startingPrice, double buyOutPrice, double tickSize, double currentHighestBid, String status, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.startingPrice = startingPrice;
        this.buyOutPrice = buyOutPrice;
        this.tickSize = tickSize;
        this.currentHighestBid = currentHighestBid;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters
    public Long getId() { return id; }
    public ItemDTO getItem() { return item; }
    public double getStartingPrice() { return startingPrice; }
    public double getBuyOutPrice() { return buyOutPrice; }
    public double getTickSize() { return tickSize; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getStatus() { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}
