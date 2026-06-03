package auctionserver.dao;

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

import auctionserver.entities.Auction;
import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionserver.exception.DatabaseException;
import auctionserver.exception.TransactionFailedException;
import auctionserver.factory.ItemFactory;
import auctionserver.interfaces.WritableDAO;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.ItemType;

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
            log.error("Lỗi cơ sở dữ liệu khi thêm mới phiên đấu giá: {}", auction.getAuctionId(), e);
            throw new DatabaseException("Không thể thêm mới phiên đấu giá: " + auction.getAuctionId(), e);
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
            log.error("Lỗi cơ sở dữ liệu khi cập nhật thời gian kết thúc phiên đấu giá: {}", auction.getAuctionId(), e);
            throw new TransactionFailedException("Không thể cập nhật thời gian kết thúc cho phiên đấu giá: " + auction.getAuctionId(), e);
        }
    }

    /**
     * Cập nhật chỉ trạng thái của auction (không thay đổi winner/bid).
     * Dùng khi Admin ban một auction — giữ nguyên toàn bộ lịch sử.
     *
     * @param auctionId ID của auction cần cập nhật
     * @param status    Trạng thái mới (thường là BANNED)
     */
    public int updateStatusOnly(String auctionId, AuctionStatus status) {
        String sql = "UPDATE auctions SET auction_status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, auctionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi cập nhật trạng thái phiên đấu giá: {}", auctionId, e);
            throw new DatabaseException("Không thể cập nhật trạng thái phiên đấu giá: " + auctionId, e);
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
     * @return danh sách Auction
     */
    public List<Auction> selectEndedSaledAuctions(String categoryFilter, int limit) {
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
                "LIMIT ?";

        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(baseSql)) {
            int paramIdx = 1;
            if (categoryFilter != null && !categoryFilter.isEmpty() && !"ALL".equals(categoryFilter)) {
                ps.setString(paramIdx++, categoryFilter);
            }
            ps.setInt(paramIdx, limit);
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
            log.error("Lỗi cơ sở dữ liệu khi truy vấn các phiên đấu giá đã kết thúc hoặc đã bán", e);
            throw new DatabaseException("Không thể truy vấn các phiên đấu giá đã kết thúc hoặc đã bán", e);
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
                        log.warn("Bỏ qua phiên đấu giá do giá trị thời gian bị rỗng (null) đối với id={}", auctionId);
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
            log.error("Lỗi cơ sở dữ liệu khi truy vấn phiên đấu giá theo ID: {}", auctionId, e);
            throw new DatabaseException("Không thể truy vấn phiên đấu giá theo ID: " + auctionId, e);
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
                    log.warn("Bỏ qua phiên đấu giá do giá trị thời gian bị rỗng (null) đối với sản phẩm có ID={}", rs.getString("item_id"));
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
            log.error("Lỗi cơ sở dữ liệu khi truy vấn các phiên đấu giá theo điều kiện", e);
            throw new DatabaseException("Không thể truy vấn các phiên đấu giá theo điều kiện", e);
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
            log.error("Lỗi cơ sở dữ liệu khi cập nhật phiên đấu giá: {}", auction.getAuctionId(), e);
            throw new DatabaseException("Không thể cập nhật phiên đấu giá: " + auction.getAuctionId(), e);
        }
    }
}
