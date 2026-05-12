package auction_server.dao;

import auction_server.entities.Auction;
import auction_server.interfaces.InterfaceDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuctionDAO implements InterfaceDAO<Auction> {
    @Override
    public int insert(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, starting_price, buy_out_price, tick_size, current_highest_bid, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int delete(Auction auction) {
        return 0;
    }

    @Override
    public int update(Auction auction) {
        return 0;
    }

    @Override
    public java.util.ArrayList<Auction> selectAll() {
        return null;
    }

    @Override
    public Auction selectById(Auction auction) {
        return null;
    }

    @Override
    public java.util.ArrayList<Auction> selectByCondition(String condition) {
        return null;
    }

}
