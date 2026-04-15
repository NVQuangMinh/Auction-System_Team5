package auction_server.behaviors;

import auction_server.entities.Item;
import auction_server.interfaces.CanSell;

public class Seller implements CanSell {
    public void postItem(Item item) {}
    public void deleteItem(){}
}
