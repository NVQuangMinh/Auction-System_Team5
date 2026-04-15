package auction_server.interfaces;

import auction_server.entities.Item;

public interface CanSell {
    void postItem(Item item);
}
