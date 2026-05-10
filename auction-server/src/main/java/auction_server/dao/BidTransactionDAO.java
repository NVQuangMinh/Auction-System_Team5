package auction_server.dao;

import auction_server.entities.BidTransaction;
import auction_server.interfaces.InterfaceDAO;

import java.util.ArrayList;

public class BidTransactionDAO implements InterfaceDAO<BidTransaction> {
    @Override
    public int insert(BidTransaction bidTransaction) {
        return 0;
    }

    @Override
    public int delete(BidTransaction bidTransaction) {
        return 0;
    }

    @Override
    public int update(BidTransaction bidTransaction) {
        return 0;
    }

    @Override
    public ArrayList<BidTransaction> selectAll() {
        return null;
    }

    @Override
    public BidTransaction selectById(BidTransaction bidTransaction) {
        return null;
    }

    @Override
    public ArrayList<BidTransaction> selectByCondition(String condition) {
        return null;
    }
}
