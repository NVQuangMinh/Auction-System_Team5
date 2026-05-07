package auction_server.dao.interfaces;

import auction_server.entities.Auction;
import java.util.List;

public interface AuctionDAO {
    Auction findById(String id); // Thêm dòng này
    void save(Auction auction);
    void update(Auction auction);
    List<Auction> findAllActive();
}