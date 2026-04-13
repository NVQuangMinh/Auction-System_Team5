package auction_system.entity;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private double currentPrice;
    private User highestBidder;
    private List<String> bidHistory = new ArrayList<>();

    public synchronized void handleNewBid(Bidder bidder, double bidAmount) {
        if (bidAmount <= currentPrice) {
            throw new IllegalArgumentException("Giá đặt phải cao hơn giá hiện tại.");
        }

        if (item.getOwner().getUsername().equals(bidder.getUsername())) {
            throw new IllegalStateException("Người bán không thể tự đấu giá sản phẩm của mình.");
        }

        if (highestBidder != null && highestBidder.getUsername().equals(bidder.getUsername())) {
            throw new IllegalStateException("Bạn đang là người giữ giá cao nhất. Vui lòng đợi người khác đặt giá.");
        }

        this.currentPrice = bidAmount;
        this.highestBidder = bidder;
        this.bidHistory.add(bidder.getUsername() + " đặt " + bidAmount);
    }

    public double getCurrentPrice() { return currentPrice; }
    public User getHighestBidder() { return highestBidder; }
}