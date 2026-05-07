package auction_server.dao.interfaces;

import auction_server.entities.Item;

public interface ItemDAO {
    Item findById(long id);
    void save(Item item);
}
