package auction_server.dao;

import auction_server.dao.interfaces.BidTransactionDAO;
import auction_server.entities.BidTransaction;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

public class BidTransactionDAOImpl implements BidTransactionDAO {
    private final DataSource dataSource;

    public BidTransactionDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Nếu id chưa được sinh ra từ hàm khởi tạo, sinh mới ở đây
            String txId = transaction.getId() != null ? transaction.getId() : UUID.randomUUID().toString();
            transaction.setId(txId);

            pstmt.setString(1, txId);
            pstmt.setString(2, transaction.getAuction().getId());
            pstmt.setString(3, transaction.getBidder().getId());
            pstmt.setDouble(4, transaction.getBidAmount());
            pstmt.setTimestamp(5, Timestamp.valueOf(transaction.getTimestamp()));

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Database error saving bid transaction", e);
        }
    }
}