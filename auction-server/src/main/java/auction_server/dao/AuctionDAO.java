package auction_server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import auction_server.entities.Auction;
import auction_server.interfaces.WritableDAO;

public class AuctionDAO implements WritableDAO<Auction> {
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
            e.printStackTrace(); // Log lỗi nhưng KHÔNG rethrow
            return 0;
        }
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
