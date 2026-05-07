package auction_server.dao.interfaces;

import auction_server.entities.BidTransaction;

public interface BidTransactionDAO {
    void save(BidTransaction transaction);
}
