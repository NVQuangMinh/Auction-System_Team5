package auction_server.dao;

import auction_server.dao.interfaces.BidTransactionDAO;
import auction_server.entities.BidTransaction;

import java.sql.*;
import java.util.UUID;

public class BidTransactionDAOImpl implements BidTransactionDAO {

    public BidTransactionDAOImpl() {}

    @Override
    public void save(BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String txId = transaction.getId() != null ? transaction.getId() : UUID.randomUUID().toString();
            transaction.setId(txId);

            pstmt.setString(1, txId);
            pstmt.setString(2, transaction.getAuction().getId());
            pstmt.setString(3, transaction.getBidder().getId());
            pstmt.setDouble(4, transaction.getBidAmount());
            pstmt.setTimestamp(5, Timestamp.valueOf(transaction.getTimestamp()));

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi lưu giao dịch đặt giá", e);
        }
    }
}