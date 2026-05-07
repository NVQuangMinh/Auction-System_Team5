package auction_server.dao;

import auction_server.entities.Auction;
import auction_server.interfaces.InterfaceDAO;

public class AuctionDAO implements InterfaceDAO<Auction> {
    @Override
    public int insert(Auction auction) {
        return 0;
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
