package auction_server.dao;

import auction_server.dao.interfaces.AuctionDAO;
import auction_server.entities.Auction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Art;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicle;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionDAOImpl implements AuctionDAO {

    private final DataSource dataSource;

    public AuctionDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Auction findById(String id) {
        String sql = "SELECT a.*, " +
                "i.name as item_name, i.description as item_desc, i.type as item_type, i.brand, i.artist_name, " +
                "u.id as owner_id, u.username, u.password_hash " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u ON i.owner_id = u.id " +
                "WHERE a.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAuction(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi tìm Auction theo ID", e);
        }
        return null;
    }

    @Override
    public List<Auction> findAllActive() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT a.*, " +
                "i.name as item_name, i.description as item_desc, i.type as item_type, i.brand, i.artist_name, " +
                "u.id as owner_id, u.username, u.password_hash " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u ON i.owner_id = u.id " +
                "WHERE a.status = 'ACTIVE'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi lấy danh sách Auction ACTIVE", e);
        }
        return auctions;
    }

    @Override
    public void save(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, starting_price, buy_out_price, tick_size, current_highest_bid, status, start_time, end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (auction.getId() == null) auction.setId(UUID.randomUUID().toString());

            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setDouble(3, auction.getStartingPrice());
            pstmt.setDouble(4, auction.getBuyOutPrice());
            pstmt.setDouble(5, auction.getTickSize());
            pstmt.setDouble(6, auction.getCurrentHighestBid());
            pstmt.setString(7, auction.getStatus());
            pstmt.setTimestamp(8, Timestamp.valueOf(auction.getCreatedAt())); // start_time
            pstmt.setTimestamp(9, Timestamp.valueOf(auction.getCreatedAt().plusDays(7))); // end_time

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi tạo phiên đấu giá", e);
        }
    }

    @Override
    public void update(Auction auction) {
        String sql = "UPDATE auctions SET current_highest_bid = ?, status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, auction.getCurrentHighestBid());
            pstmt.setString(2, auction.getStatus());
            pstmt.setString(3, auction.getId());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi DB khi cập nhật trạng thái đấu giá", e);
        }
    }

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        User owner = new User(rs.getString("owner_id"), rs.getString("username"), rs.getString("password_hash"));

        Item item;
        String type = rs.getString("item_type").toLowerCase();
        String itemId = rs.getString("item_id");
        String name = rs.getString("item_name");
        String desc = rs.getString("item_desc");

        if (type.equals("art")) {
            item = new Art(itemId, name, desc, owner, rs.getString("artist_name"));
        } else if (type.equals("electronics")) {
            item = new Electronics(itemId, name, desc, owner, rs.getString("brand"));
        } else {
            item = new Vehicle(itemId, name, desc, owner, rs.getString("brand"));
        }

        return new Auction(
                rs.getString("id"),
                item,
                rs.getDouble("starting_price"),
                rs.getDouble("buy_out_price"),
                rs.getDouble("tick_size"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime()
        );
    }
}