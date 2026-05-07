package auction_server.dao;

import auction_server.dao.interfaces.AuctionDAO;
import auction_server.entities.Auction;
import auction_server.entities.Item;
import auction_server.entities.items.Art;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAOImpl implements AuctionDAO {

    private final DataSource dataSource;

    public AuctionDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Auction findById(long id) {
        // TODO: Hoàn thiện câu lệnh SQL. Cần JOIN với bảng Item và User
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAuction(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding auction by id", e);
        }
        return null;
    }

    @Override
    public void update(Auction auction) {
        // Câu lệnh này không thay đổi vì ta chỉ cập nhật giá và trạng thái
        String sql = "UPDATE auctions SET current_highest_bid = ?, status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, auction.getCurrentHighestBid());
            pstmt.setString(2, auction.getStatus());
            pstmt.setLong(3, auction.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Database error updating auction", e);
        }
    }

    @Override
    public List<Auction> findAllActive() {
        List<Auction> auctions = new ArrayList<>();
        // TODO: Hoàn thiện câu lệnh SQL. Cần JOIN với bảng Item và User
        String sql = "SELECT * FROM auctions WHERE status = 'ACTIVE'";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding all active auctions", e);
        }
        return auctions;
    }

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        // TODO: Cần fetch đầy đủ đối tượng Item từ DB, đây là ví dụ tạm thời
        Item tempItem = new Art(rs.getLong("item_id"), "temp", "", null, "");

        return new Auction(
                rs.getLong("id"),
                tempItem,
                rs.getDouble("starting_price"),
                rs.getDouble("buy_out_price"), // Mới
                rs.getDouble("tick_size"),       // Mới
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime()
        );
    }
}
