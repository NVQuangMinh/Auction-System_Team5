package auction_server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auction_server.entities.Auction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.exception.DatabaseException;
import auction_server.exception.TransactionFailedException;
import auction_server.factory.ItemFactory;
import auction_server.interfaces.WritableDAO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.ItemType;

@SuppressWarnings("rawtypes")
public class AuctionDAO implements WritableDAO<Auction> {
    private static final Logger log = LoggerFactory.getLogger(AuctionDAO.class);

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
            log.error("Database error while inserting auction: {}", auction.getAuctionId(), e);
            throw new DatabaseException("Failed to insert auction: " + auction.getAuctionId(), e);
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
            log.error("Database error while updating end time for auction: {}", auction.getAuctionId(), e);
            throw new TransactionFailedException("Failed to update end time for auction: " + auction.getAuctionId(), e);
        }
    }

    /**
     * Load tất cả auction có trạng thái ACTIVE từ DB.
     * Dùng khi server khởi động lại để rebuild in-memory state.
     */
    public List<Auction> selectActiveAuctions() {
        return selectByCondition("a.auction_status = 'ACTIVE'");
    }

    /**
     * Load tất cả auction (ACTIVE, ENDED, SOLD) từ DB.
     * Dùng để hiển thị full danh sách bao gồm cả auction đã kết thúc.
     */
    public List<Auction> selectAllAuctions() {
        return selectByCondition("1=1");
    }

    /**
     * Load auction ENDED/SOLD từ DB với phân trang và lọc theo category.
     * ACTIVE auctions được serve từ RAM, không query DB.
     *
     * @param categoryFilter "ALL", "ARTS", "ELECTRONICS", "VEHICLES" hoặc null
     *                       (ALL)
     * @param page           số trang (0-based)
     * @param pageSize       số item mỗi trang
     * @return danh sách Auction
     */
    public List<Auction> selectEndedSaledAuctions(String categoryFilter, int page, int pageSize) {
        String statusCondition = "(a.auction_status = 'ENDED' OR a.auction_status = 'SOLD')";
        String categoryCondition = "";
        if (categoryFilter != null && !categoryFilter.isEmpty() && !"ALL".equals(categoryFilter)) {
            categoryCondition = " AND i.item_type = ? ";
        }
        String baseSql = "SELECT a.*, " +
                "i.id as item_id, i.item_name, i.description, i.item_type, " +
                "i.artist_name, i.model, i.brand, " +
                "u.id as owner_id, u.username " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u ON i.owner_id = u.id " +
                "WHERE " + statusCondition + categoryCondition +
                " ORDER BY a.end_time DESC " +
                "LIMIT ? OFFSET ?";

        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(baseSql)) {
            int paramIdx = 1;
            if (categoryFilter != null && !categoryFilter.isEmpty() && !"ALL".equals(categoryFilter)) {
                ps.setString(paramIdx++, categoryFilter);
            }
            ps.setInt(paramIdx++, pageSize);
            ps.setInt(paramIdx, page * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User owner = new User(
                            rs.getString("owner_id"),
                            rs.getString("username"),
                            null);
                    Item item = mapRowToItem(rs, owner);

                    LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                    LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();

                    if (startTime == null || endTime == null) {
                        continue;
                    }

                    auctions.add(new Auction(
                            item,
                            rs.getDouble("starting_price"),
                            rs.getDouble("buy_out_price"),
                            rs.getDouble("tick_size"),
                            startTime,
                            endTime,
                            rs.getBoolean("anti_snipe"),
                            rs.getDouble("current_highest_bid"),
                            AuctionStatus.valueOf(rs.getString("auction_status"))));
                }
            }
        } catch (SQLException e) {
            log.error("Database error while selecting ended/sold auctions", e);
            throw new DatabaseException("Failed to select ended/sold auctions", e);
        }
        return auctions;
    }

    /**
     * Đếm tổng số auction ENDED/SOLD theo category filter.
     * Dùng để tính tổng số trang.
     */
    public int countEndedSaledAuctions(String categoryFilter) {
        String statusCondition = "(a.auction_status = 'ENDED' OR a.auction_status = 'SOLD')";
        String categoryCondition = "";
        if (categoryFilter != null && !categoryFilter.isEmpty() && !"ALL".equals(categoryFilter)) {
            categoryCondition = " AND i.item_type = ? ";
        }
        String baseSql = "SELECT COUNT(*) FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "WHERE " + statusCondition + categoryCondition;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(baseSql)) {
            if (categoryFilter != null && !categoryFilter.isEmpty() && !"ALL".equals(categoryFilter)) {
                ps.setString(1, categoryFilter);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } else {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Database error while counting ended/sold auctions", e);
        }
        return 0;
    }

    /**
     * Helper: map một dòng ResultSet thành Item entity.
     */
    private Item mapRowToItem(ResultSet rs, User owner) throws SQLException {
        String id = rs.getString("item_id");
        String name = rs.getString("item_name");
        String description = rs.getString("description");
        ItemType type = ItemType.fromDbValue(rs.getString("item_type"));
        return ItemFactory.of(type).create(id, name, description, owner,
                rs.getString(type.attributeColumn()));
    }

    @Override
    public int delete(Auction auction) {
        return 0;
    }

    public Auction selectById(String auctionId) {
        String sql = "SELECT a.*, " +
                "i.id as item_id, i.item_name, i.description, i.item_type, " +
                "i.artist_name, i.model, i.brand, " +
                "u.id as owner_id, u.username, u.password " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u ON i.owner_id = u.id " +
                "WHERE a.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User owner = new User(
                            rs.getString("owner_id"),
                            rs.getString("username"),
                            rs.getString("password"));
                    Item item = mapRowToItem(rs, owner);

                    LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                    LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                    if (startTime == null || endTime == null) {
                        log.warn("Skipping auction with null timestamps for id={}", auctionId);
                        return null;
                    }

                    return new Auction(
                            item,
                            rs.getDouble("starting_price"),
                            rs.getDouble("buy_out_price"),
                            rs.getDouble("tick_size"),
                            startTime,
                            endTime,
                            rs.getBoolean("anti_snipe"),
                            rs.getDouble("current_highest_bid"),
                            AuctionStatus.valueOf(rs.getString("auction_status")));
                }
            }
        } catch (SQLException e) {
            log.error("Database error while selecting auction by id: {}", auctionId, e);
            throw new DatabaseException("Failed to select auction: " + auctionId, e);
        }
        return null;
    }

    public List<Auction> selectByCondition(String condition) {
        String sql = "SELECT a.*, " +
                "i.id as item_id, i.item_name, i.description, i.item_type, " +
                "i.artist_name, i.model, i.brand, " +
                "u.id as owner_id, u.username, u.password " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.id " +
                "JOIN users u ON i.owner_id = u.id " +
                "WHERE " + condition;

        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User owner = new User(
                        rs.getString("owner_id"),
                        rs.getString("username"),
                        rs.getString("password"));
                Item item = mapRowToItem(rs, owner);

                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                if (startTime == null || endTime == null) {
                    log.warn("Skipping auction with null timestamps for item_id={}", rs.getString("item_id"));
                    continue;
                }

                auctions.add(new Auction(
                        item,
                        rs.getDouble("starting_price"),
                        rs.getDouble("buy_out_price"),
                        rs.getDouble("tick_size"),
                        startTime,
                        endTime,
                        rs.getBoolean("anti_snipe"),
                        rs.getDouble("current_highest_bid"),
                        AuctionStatus.valueOf(rs.getString("auction_status"))));
            }
        } catch (SQLException e) {
            log.error("Database error while selecting auctions by condition", e);
            throw new DatabaseException("Failed to select auctions", e);
        }
        return auctions;
    }

    @Override
    public int update(Auction auction) {
        // Cập nhật status và winner_id khi phiên đấu giá kết thúc (được gọi bởi
        // Scheduler)
        String sql = "UPDATE auctions SET auction_status = ?, winner_id = ?, current_highest_bid = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, auction.getStatus().name());
            ps.setString(2, auction.getWinnerId());
            ps.setDouble(3, auction.getCurrentHighestBid());
            ps.setString(4, auction.getAuctionId());

            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Database error while updating auction: {}", auction.getAuctionId(), e);
            throw new DatabaseException("Failed to update auction: " + auction.getAuctionId(), e);
        }
    }
}
