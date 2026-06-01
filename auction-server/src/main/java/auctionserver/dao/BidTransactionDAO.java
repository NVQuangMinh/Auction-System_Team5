package auctionserver.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auctionserver.entities.BidTransaction;
import auctionserver.entities.User;
import auctionserver.exception.DatabaseException;
import auctionserver.interfaces.TransactionalDAO;

public class BidTransactionDAO implements TransactionalDAO<BidTransaction> {
    private static final Logger log = LoggerFactory.getLogger(BidTransactionDAO.class);

    @Override
    public int insert(BidTransaction bt, Connection conn) throws SQLException {
        String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, bid_time) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bt.getId());
            ps.setString(2, bt.getAuction().getAuctionId());
            ps.setString(3, bt.getBidder().getId());
            ps.setDouble(4, bt.getBidAmount());
            ps.setTimestamp(5, Timestamp.valueOf(bt.getBidTime()));
            return ps.executeUpdate();
        }
    }

    public BidTransaction findTopBidderByAuction(String auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String bidderId = rs.getString("bidder_id");
                    User bidder = new User(bidderId, null, null);

                    BidTransaction transaction = new BidTransaction(
                            null,
                            bidder,
                            rs.getDouble("bid_amount"));
                    transaction.setId(rs.getString("id"));
                    Timestamp timestamp = rs.getTimestamp("bid_time");
                    if (timestamp != null) {
                        transaction.setBidTime(timestamp.toLocalDateTime());
                    }

                    return transaction;
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi tìm người trả giá cao nhất cho phiên đấu giá: {}", auctionId, e);
            throw new DatabaseException("Không thể tìm người trả giá cao nhất cho phiên đấu giá: " + auctionId, e);
        }
        return null;
    }

    public Map<String, List<BidTransaction>> selectActiveAuctionsBidHistory() {
        Map<String, List<BidTransaction>> resultMap = new HashMap<>();

        String sql = "SELECT bt.*, u.username, u.role, u.user_status " +
                "FROM bid_transactions bt " +
                "JOIN users u ON bt.bidder_id = u.id " +
                "JOIN auctions a ON bt.auction_id = a.id " +
                "WHERE a.auction_status = 'ACTIVE' " +
                "ORDER BY bt.bid_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String auctionId = rs.getString("auction_id");

                User bidder = new User(
                        rs.getString("bidder_id"),
                        rs.getString("username"),
                        null,
                        rs.getString("role"),
                        rs.getString("user_status"));

                BidTransaction tx = new BidTransaction(null, bidder, rs.getDouble("bid_amount"));
                tx.setId(rs.getString("id"));
                Timestamp timestamp = rs.getTimestamp("bid_time");
                if (timestamp != null) {
                    tx.setBidTime(timestamp.toLocalDateTime());
                }

                resultMap.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(tx);
            }
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi truy vấn lịch sử trả giá của các phiên đấu giá đang diễn ra", e);
            throw new DatabaseException("Không thể tìm lịch sử trả giá của các phiên đấu giá đang diễn r", e);
        }
        return resultMap;
    }

    /**
     * Đọc full bid history của một auction từ DB.
     * JOIN với users để lấy username của bidder.
     * Sắp xếp theo bid_time ASC để biểu đồ đi lên đúng thứ tự.
     *
     * Được sử dụng khi auction đã ENDED/SOLD (không còn trong RAM).
     */
    public ArrayList<BidTransaction> selectByAuctionId(String auctionId) {
        String sql = "SELECT bt.*, u.username, u.role, u.user_status " +
                "FROM bid_transactions bt " +
                "JOIN users u ON bt.bidder_id = u.id " +
                "WHERE bt.auction_id = ? " +
                "ORDER BY bt.bid_time ASC";

        ArrayList<BidTransaction> result = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auctionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User bidder = new User(
                            rs.getString("bidder_id"),
                            rs.getString("username"),
                            null,
                            rs.getString("role"),
                            rs.getString("user_status"));

                    BidTransaction tx = new BidTransaction(
                            null,
                            bidder,
                            rs.getDouble("bid_amount"));
                    tx.setId(rs.getString("id"));
                    Timestamp timestamp = rs.getTimestamp("bid_time");
                    if (timestamp != null) {
                        tx.setBidTime(timestamp.toLocalDateTime());
                    }
                    result.add(tx);
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi truy vấn các giao dịch trả giá của phiên đấu giá: {}", auctionId, e);
            throw new DatabaseException("Không thể tìm các giao dịch trả giá của phiên đấu giá: " + auctionId, e);
        }
        return result;
    }
}
