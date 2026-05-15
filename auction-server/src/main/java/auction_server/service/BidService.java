package auction_server.service;

import auction_server.dao.AuctionDAO;
import auction_server.dao.BidTransactionDAO;
import auction_server.dao.DatabaseConnection;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class BidService {
    private static final Logger log = LoggerFactory.getLogger(BidService.class);
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidTransactionDAO bidDAO = new BidTransactionDAO();

    /**
     * Truy vấn fallback: tìm winner_id từ DB khi server restart.
     * Service nhận Entity từ DAO và xử lý logic trích xuất dữ liệu.
     */
    public String findWinnerId(String auctionId) {
        BidTransaction topTx = bidDAO.findTopBidderByAuction(auctionId);
        if (topTx != null && topTx.getBidder() != null) {
            return topTx.getBidder().getId();
        }
        return null;
    }

    public boolean processAndSaveBid(Auction auction, BidTransaction transaction) {
        if (!auction.placeBid(transaction)) {
            return false;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                bidDAO.insert(transaction, conn);
                auctionDAO.updateHighestBid(auction, conn);

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                log.error("Lỗi Transaction DB khi lưu Bid, đang rollback cả DB và RAM...", e);

                // Đã có giải pháp cho dòng "// dunno": Hoàn tác in-memory state
                auction.revertLastBid(transaction);
                return false;
            }
        } catch (SQLException e) {
            log.error("Không thể lấy Connection DB", e);
            // Lỗi kết nối DB, in-memory cũng phải hoàn tác
            auction.revertLastBid(transaction);
            return false;
        }
    }

    /**
     * Xử lý Buy Out với đầy đủ DB Transaction.
     * Luồng: validate in-memory (auction.buyOut) -> mở Transaction DB ->
     * insert BidTransaction + update Auction status/winner -> commit.
     * Nếu DB lỗi: rollback DB + hoàn tác in-memory state.
     *
     * @return true nếu Buy Out thành công (cả RAM và DB đều nhất quán)
     */
    public boolean processBuyOut(Auction auction, BidTransaction transaction) {
        // Bước 1: Validate và cập nhật in-memory state
        if (!auction.buyOut(transaction)) {
            return false;
        }

        // Bước 2: Persist xuống DB trong một Transaction
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert giao dịch buy out vào bảng bid_transactions
                bidDAO.insert(transaction, conn);
                // Cập nhật status = SOLD, winner_id, current_highest_bid vào bảng auctions
                auctionDAO.updateStatusAndWinner(auction, conn);

                conn.commit();
                log.info("Buy Out thành công: Auction={}, Winner={}",
                        auction.getAuctionId(), auction.getWinnerId());
                return true;
            } catch (SQLException e) {
                conn.rollback();
                log.error("Lỗi Transaction DB khi Buy Out, đang rollback cả DB và RAM...", e);

                // Hoàn tác in-memory: đặt lại status về ACTIVE, trả owner về chủ cũ
                auction.revertBuyOut(transaction);
                return false;
            }
        } catch (SQLException e) {
            log.error("Không thể lấy Connection DB cho Buy Out", e);
            auction.revertBuyOut(transaction);
            return false;
        }
    }
}