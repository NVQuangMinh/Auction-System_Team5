package auction_server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import auction_server.entities.Auction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicles;
import auction_server.interfaces.WritableDAO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.ItemType;

public class AuctionDAO implements WritableDAO<Auction> {

    public Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }
    @Override
    public int insert(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, starting_price, buy_out_price, tick_size, current_highest_bid, start_time, end_time, auction_status, anti_snipe) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String auctionId = auction.getItem().getId();
            pstmt.setString(1, auctionId);
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setDouble(3, auction.getStartingPrice());
            pstmt.setDouble(4, auction.getBuyOutPrice());
            pstmt.setDouble(5, auction.getTickSize());
            pstmt.setDouble(6, auction.getCurrentHighestBid());
            pstmt.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setString(9, auction.getStatus().name());
            pstmt.setBoolean(10, auction.isAntiSniping());
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int insert(Auction auction, Connection conn) throws SQLException {
        String sql = "INSERT INTO auctions (id, item_id, starting_price, buy_out_price, tick_size, current_highest_bid, start_time, end_time, auction_status, anti_snipe) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String auctionId = auction.getItem().getId();
            pstmt.setString(1, auctionId);
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setDouble(3, auction.getStartingPrice());
            pstmt.setDouble(4, auction.getBuyOutPrice());
            pstmt.setDouble(5, auction.getTickSize());
            pstmt.setDouble(6, auction.getCurrentHighestBid());
            pstmt.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setString(9, auction.getStatus().name());
            pstmt.setBoolean(10, auction.isAntiSniping());
            return pstmt.executeUpdate();
        }
    }

    public int updateHighestBid(Auction auction, Connection conn) throws SQLException {
        String sql = "UPDATE auctions SET current_highest_bid = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, auction.getCurrentHighestBid());
            ps.setString(2, auction.getAuctionId());
            return ps.executeUpdate();
        }
    }

    /**
     * Cập nhật trạng thái, winner và giá cao nhất trong cùng một DB Transaction.
     * Được sử dụng bởi BidService.processBuyOut() để đảm bảo tính toàn vẹn dữ liệu.
     */
    public int updateStatusAndWinner(Auction auction, Connection conn) throws SQLException {
        String sql = "UPDATE auctions SET auction_status = ?, winner_id = ?, current_highest_bid = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getStatus().name());
            ps.setString(2, auction.getWinnerId());
            ps.setDouble(3, auction.getCurrentHighestBid());
            ps.setString(4, auction.getAuctionId());
            return ps.executeUpdate();
        }
    }

    public int updateEndTime(Auction auction, Connection conn) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(2, auction.getAuctionId());
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Load tất cả auction có trạng thái ACTIVE từ DB.
     * Dùng khi server khởi động lại để rebuild in-memory state.
     */
    public List<Auction> selectActiveAuctions() {
        List<Auction> auctions = new ArrayList<>();

        String sql = "SELECT a.*, " +
                "i.id as item_id, i.item_name, i.description, i.item_type, " +
                "u.id as owner_id, u.username, u.password " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u ON i.owner_id = u.id " +
                "WHERE a.auction_status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User owner = new User(
                        rs.getString("owner_id"),
                        rs.getString("username"),
                        rs.getString("password")
                );

                Item item = mapRowToItem(rs, owner);

                Auction auction = new Auction(
                        item,
                        rs.getDouble("starting_price"),
                        rs.getDouble("buy_out_price"),
                        rs.getDouble("tick_size"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getBoolean("anti_snipe"),
                        rs.getDouble("current_highest_bid"),
                        AuctionStatus.ACTIVE
                );

                auctions.add(auction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }

    /**
     * Helper: map một dòng ResultSet thành Item entity.
     */
    private Item mapRowToItem(ResultSet rs, User owner) throws SQLException {
        String id = rs.getString("item_id");
        String name = rs.getString("item_name");
        String description = rs.getString("description");
        ItemType type = ItemType.fromDbValue(rs.getString("item_type"));
        return switch (type) {
            case ARTS -> new Arts(id, name, description, owner);
            case ELECTRONICS -> new Electronics(id, name, description, owner);
            case VEHICLES -> new Vehicles(id, name, description, owner);
        };
    }

    @Override
    public int delete(Auction auction) {
        return 0;
    }

    @Override
    public int update(Auction auction) {
        // Cập nhật status và winner_id khi phiên đấu giá kết thúc (được gọi bởi Scheduler)
        String sql = "UPDATE auctions SET auction_status = ?, winner_id = ?, current_highest_bid = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auction.getStatus().name());
            ps.setString(2, auction.getWinnerId());
            ps.setDouble(3, auction.getCurrentHighestBid());
            ps.setString(4, auction.getAuctionId());

            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
