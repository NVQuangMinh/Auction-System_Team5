package auction_server.dao;

import auction_server.dao.interfaces.BidTransactionDAO;
import auction_server.entities.BidTransaction;

import javax.sql.DataSource;
import java.sql.*;

public class BidTransactionDAOImpl implements BidTransactionDAO {

    private final DataSource dataSource;

    public BidTransactionDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(BidTransaction transaction) {
        // TODO: Hoàn thiện câu lệnh SQL
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, transaction.getAuction().getId());
            pstmt.setLong(2, transaction.getBidder().getId());
            pstmt.setDouble(3, transaction.getBidAmount());
            pstmt.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setId(generatedKeys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error saving bid transaction", e);
        }
    }
}
