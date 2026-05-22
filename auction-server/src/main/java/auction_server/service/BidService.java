package auction_server.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import auction_server.exception.BidException;
import auction_server.exception.DatabaseException;
import auction_server.exception.InactiveBidException;
import auction_server.exception.InvalidBidAmountException;
import auction_server.exception.SelfBiddingException;
import auction_server.exception.TransactionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auction_server.dao.AuctionDAO;
import auction_server.dao.BidTransactionDAO;
import auction_server.dao.DAOProvider;
import auction_server.dao.DatabaseConnection;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;

public class BidService {
    private static final Logger log = LoggerFactory.getLogger(BidService.class);
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidDAO;

    public BidService(DAOProvider daoProvider) {
        this.auctionDAO = daoProvider.auctionDAO();
        this.bidDAO = daoProvider.bidTransactionDAO();
    }

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

    public void processAndSaveBid(Auction auction, BidTransaction transaction) throws BidException {
        auction.placeBid(transaction);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                bidDAO.insert(transaction, conn);
                auctionDAO.updateHighestBid(auction, conn);

                conn.commit();
                // Anti-sniping: gia hạn nếu còn <= 30s
                if (auction.isAntiSniping()) {
                    LocalDateTime oldEndTime = auction.getEndTime();
                    auction.extendTime();
                    if (!oldEndTime.equals(auction.getEndTime())) {
                        auctionDAO.updateEndTime(auction, conn);
                    }
                }

                log.info("Bid thành công: Auction={}, Bidder={}, BidAmount={}",
                        auction.getAuctionId(), transaction.getBidder().getUsername(), transaction.getBidAmount());
            } catch (SQLException e) {
                conn.rollback();
                log.error("Lỗi Transaction DB khi lưu Bid, đang rollback cả DB và RAM...", e);
                auction.revertLastBid(transaction);
                throw new TransactionFailedException("Failed to save bid transaction", e);
            } catch (Exception e) {
                conn.rollback();
                log.error("Unexpected exception occurred while processing bid", e);
                auction.revertLastBid(transaction);
                throw new TransactionFailedException("Unexpected error while processing bid", e);
            }
        } catch (SQLException e) {
            log.error("Không thể lấy Connection DB", e);
            auction.revertLastBid(transaction);
            throw new DatabaseException("Failed to get database connection", e);
        } catch (Exception e) {
            log.error("Unexpected exception occurred while getting database connection", e);
            auction.revertLastBid(transaction);
            throw new DatabaseException("Unexpected error while getting database connection", e);
        }
    }

    /**
     * Xử lý Buy Out với đầy đủ DB Transaction.
     * Luồng: validate in-memory (auction.buyOut) -> mở Transaction DB ->
     * insert BidTransaction + update Auction status/winner -> commit.
     * Nếu DB lỗi: rollback DB + hoàn tác in-memory state.
     */
    public void processBuyOut(Auction auction, BidTransaction transaction) throws BidException {
        // Validate và cập nhật in-memory state
        auction.buyOut(transaction);
        // Persist xuống DB trong một Transaction
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                bidDAO.insert(transaction, conn);
                auctionDAO.updateStatusAndWinner(auction, conn);

                conn.commit();
                log.info("Buy Out thành công: Auction={}, Winner={}",
                        auction.getAuctionId(), auction.getWinnerId());
            } catch (SQLException e) {
                conn.rollback();
                log.error("Lỗi Transaction DB khi Buy Out, đang rollback cả DB và RAM...", e);
                auction.revertBuyOut(transaction);
                throw new TransactionFailedException("Failed to process buy-out transaction", e);
            } catch (Exception e) {
                conn.rollback();
                log.error("Unexpected exception occurred while processing buy-out", e);
                auction.revertBuyOut(transaction);
                throw new TransactionFailedException("Unexpected error while processing buy-out", e);
            }
        } catch (SQLException e) {
            log.error("Không thể lấy Connection DB cho Buy Out", e);
            auction.revertBuyOut(transaction);
            throw new DatabaseException("Failed to get database connection", e);
        } catch (Exception e) {
            log.error("Unexpected exception occurred while getting database connection for buy-out", e);
            auction.revertBuyOut(transaction);
            throw new DatabaseException("Unexpected error while getting database connection", e);
        }
    }
}