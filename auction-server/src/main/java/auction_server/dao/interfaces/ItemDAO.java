package auction_server.dao.interfaces;

import auction_server.entities.Item;

public interface ItemDAO {
    Item findById(String id);

    void save(Item item);
}