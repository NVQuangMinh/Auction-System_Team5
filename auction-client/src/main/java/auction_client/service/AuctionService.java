package auction_client.service;

import auction_client.entity.Auction;
import auction_client.entity.BidTransaction;
import auction_client.entity.Bidder;
import auction_client.exception.AuctionClosedException;
import auction_client.exception.InvalidBidException;

import java.time.LocalDateTime;

public class AuctionService {

    public void placeBid(Auction auction, Bidder bidder, double bidAmount) {

        auction.getLock().lock();

        try {
            if (!auction.getStatus().equals("RUNNING")) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc hoặc chưa mở.");
            }

            if (auction.getItem().getSeller().getUsername().equals(bidder.getUsername())) {
                throw new InvalidBidException("Bạn không thể tự đấu giá cho sản phẩm do chính mình đăng bán.");
            }

            if (auction.getHighestBidder() != null && auction.getHighestBidder().getUsername().equals(bidder.getUsername())) {
                throw new InvalidBidException("Bạn hiện đang giữ mức giá cao nhất. Vui lòng đợi người khác đặt giá.");
            }

            double minimumRequiredBid = auction.getCurrentHighestBid() + auction.getBidIncrement();
            if (bidAmount < minimumRequiredBid) {
                throw new InvalidBidException("Giá đặt phải lớn hơn hoặc bằng " + minimumRequiredBid + " (Bước giá: " + auction.getBidIncrement() + ")");
            }

            // TODO: [DATABASE] Gọi class DAO (ví dụ: BidTransactionDao.insert(transaction)) để lưu xuống Cloud Database tại đây.

            auction.setCurrentHighestBid(bidAmount);
            auction.setHighestBidder(bidder);

            BidTransaction newTransaction = new BidTransaction(auction, bidder, bidAmount, LocalDateTime.now());
            auction.addTransaction(newTransaction);

            // TODO: [OBSERVER PATTERN] Lấy Instance của Socket Server và phát (Broadcast) gói tin chứa giá mới đến tất cả các Client đang theo dõi phiên này.

        } finally {
            auction.getLock().unlock();
        }
    }

    public void closeAuction(Auction auction) {
        auction.getLock().lock();
        try {
            auction.setStatus("FINISHED");

            // TODO: [DATABASE] Gọi DAO cập nhật trạng thái của Auction thành FINISHED trong Cloud Database.
            // TODO: [LOGIC] Kiểm tra người thắng cuộc (nếu auction.getHighestBidder() != null) và tạo đơn hàng.

        } finally {
            auction.getLock().unlock();
        }
    }
}