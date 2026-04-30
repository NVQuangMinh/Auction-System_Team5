package auction_shared.interfaces;

import auction_server.entities.Item;

public interface SellerAction {
    void postItem(Item item);
}
