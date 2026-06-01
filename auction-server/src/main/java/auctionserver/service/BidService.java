package auctionserver.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import auctionserver.exception.BidException;
import auctionserver.exception.TransactionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auctionserver.dao.AuctionDAO;
import auctionserver.dao.BidTransactionDAO;
import auctionserver.dao.DAOProvider;
import auctionserver.dao.DatabaseConnection;
import auctionserver.entities.Auction;
import auctionserver.entities.BidTransaction;

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

    /**
     * Xử lý đặt bid với lock từ đầu đến cuối operation.
     * Luồng: acquire lock -> validate + update RAM -> DB transaction -> release lock.
     * Nếu DB lỗi: rollback DB + hoàn tác RAM state (revertLastBid tự lock lại).
     */
    public void processAndSaveBid(Auction auction, BidTransaction transaction) throws BidException {
        auction.getLock().lock();
        try {
            auction.prepareBidInMemory(transaction);

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    bidDAO.insert(transaction, conn);
                    auctionDAO.updateHighestBid(auction, conn);

                    if (auction.isAntiSniping()) {
                        LocalDateTime oldEndTime = auction.getEndTime();
                        auction.extendTime();
                        if (!oldEndTime.equals(auction.getEndTime())) {
                            auctionDAO.updateEndTime(auction, conn);
                        }
                    }
                    conn.commit();

                    log.info("Trả giá sản phẩm thành công: Auction={}, Bidder={}, BidAmount={}",
                            auction.getAuctionId(), transaction.getBidder().getUsername(), transaction.getBidAmount());
                } catch (SQLException e) {
                    conn.rollback();
                    throw new TransactionFailedException("Không thể lưu giao dịch trả giá (bid transaction).", e);
                }
            }
        } catch (TransactionFailedException e) {
            log.error("Lỗi Transaction DB khi lưu Bid, đang hoàn tác RAM...", e);
            auction.revertLastBid(transaction);
            throw e;
        } catch (BidException e) {
            log.error("Xác thực lượt trả giá sản phẩm thất bại: Auction={}", auction.getAuctionId(), e);
            auction.revertLastBid(transaction);
            throw e;
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi trong quá trình xử lý lượt trả giá sản phẩm.", e);
            auction.revertLastBid(transaction);
            throw new TransactionFailedException("Xảy ra lỗi trong quá trình xử lý lượt trả giá sản phẩm.", e);
        } finally {
            auction.getLock().unlock();
        }
    }

    /**
     * Xử lý Buy Out với lock từ đầu đến cuối operation.
     * Luồng: acquire lock -> validate + update RAM -> DB transaction -> release lock.
     * Nếu DB lỗi: rollback DB + hoàn tác RAM state.
     */
    public void processBuyOut(Auction auction, BidTransaction transaction) throws BidException {
        auction.getLock().lock();
        try {
            auction.buyOut(transaction);

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    bidDAO.insert(transaction, conn);
                    auctionDAO.updateStatusAndWinner(auction, conn);
                    conn.commit();

                    log.info("Mua sản phẩm thành công: Auction={}, Winner={}",
                            auction.getAuctionId(), auction.getWinnerId());
                } catch (SQLException e) {
                    conn.rollback();
                    throw new TransactionFailedException("Không thể xử lí giao dịch mua sản phẩm", e);
                }
            }
        } catch (TransactionFailedException e) {
            log.error("Lỗi Transaction DB khi Buy Out, đang hoàn tác RAM...", e);
            auction.revertBuyOut(transaction);
            throw e;
        } catch (BidException e) {
            log.error("Xác thực lượt mua sản phẩm thất bại: Auction={}", auction.getAuctionId(), e);
            auction.revertBuyOut(transaction);
            throw e;
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi trong quá trình xử lý lượt mua sản phẩm", e);
            auction.revertBuyOut(transaction);
            throw new TransactionFailedException("Xxảy ra lỗi trong quá trình xử lý lượt mua sản phẩm", e);
        } finally {
            auction.getLock().unlock();
        }
    }
}
