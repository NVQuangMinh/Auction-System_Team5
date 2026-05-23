package auction_server.entities;

import auction_server.exception.BidException;
import auction_server.exception.InactiveBidException;
import auction_server.exception.InvalidBidAmountException;
import auction_server.exception.SelfBiddingException;
import auction_shared.dto.AuctionStatus;

public class AuctionValidator {

    public static void validateBid(Auction auction, BidTransaction transaction) throws BidException {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new InactiveBidException("Auction đã kết thúc!");
        }
        if (auction.isExpired()) {
            throw new InactiveBidException("Auction đã hết hạn chốt!");
        }
        if (auction.getItem().getOwner().getUsername().equals(transaction.getBidder().getUsername())) {
            throw new SelfBiddingException("Người đấu giá không được là người bán hàng!");
        }
        double bidAmount = transaction.getBidAmount();
        if (bidAmount <= auction.getCurrentHighestBid()) {
            throw new InvalidBidAmountException("Giá đặt phải lớn hơn giá hiện tại!");
        }
        if (bidAmount >= auction.getBuyOutPrice()) {
            throw new InvalidBidAmountException("Giá đặt phải nhỏ hơn giá mua ngay!");
        }
        double increment = bidAmount - auction.getCurrentHighestBid();
        long ticks = Math.round(increment / auction.getTickSize());
        if (ticks <= 0 || Math.abs(increment - ticks * auction.getTickSize()) > 0.001) {
            throw new InvalidBidAmountException("Giá đặt không hợp lệ!");
        }
    }

    public static void validateBuyOut(Auction auction, BidTransaction transaction) throws BidException {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new InactiveBidException("Auction đã kết thúc!");
        }
        if (auction.isExpired()) {
            throw new InactiveBidException("Auction đã hết hạn chốt!");
        }
        if (transaction.getBidder().getUsername().equals(auction.getItem().getOwner().getUsername())) {
            throw new SelfBiddingException("Người đấu giá không được là người bán hàng!");
        }
        if (Math.abs(transaction.getBidAmount() - auction.getBuyOutPrice()) > 0.001) {
            throw new InvalidBidAmountException("Giá mua ngay không hợp lệ! Phải đúng bằng giá buyOutPrice = " + auction.getBuyOutPrice());
        }
    }
}
