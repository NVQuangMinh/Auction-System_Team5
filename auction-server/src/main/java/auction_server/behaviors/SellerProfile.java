package auction_server.behaviors;

import auction_server.entities.Item;
import auction_server.interfaces.SellerAction;

import java.io.Serializable;

public class SellerProfile implements SellerAction, Serializable {
    public void postItem(Item item) {}
}
