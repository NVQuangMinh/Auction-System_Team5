package auction_server.dao;

import auction_server.entities.BidTransaction;
import auction_server.entities.User;
import auction_server.interfaces.InterfaceDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class BidTransactionDAO implements InterfaceDAO<BidTransaction> {

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
                            null, // không object
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
            e.printStackTrace(); // Trong thực tế nên dùng Logger
        }
        return null;
    }

    @Override
    public int insert(BidTransaction bidTransaction) {
        return 0;
    }

    @Override
    public int delete(BidTransaction bidTransaction) {
        return 0;
    }

    @Override
    public int update(BidTransaction bidTransaction) {
        return 0;
    }

    @Override
    public ArrayList<BidTransaction> selectAll() {
        return null;
    }

    @Override
    public BidTransaction selectById(BidTransaction bidTransaction) {
        return null;
    }

    @Override
    public ArrayList<BidTransaction> selectByCondition(String condition) {
        return null;
    }
}
