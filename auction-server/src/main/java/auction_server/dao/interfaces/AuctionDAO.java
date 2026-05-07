package auction_server.dao.interfaces;

import auction_server.entities.Auction;
import java.util.List;

public interface AuctionDAO {
    void update(Auction auction);
    List<Auction> findAllActive();
    // TODO: Consider adding findById, save, etc. as needed
}
