package auction_shared.behaviors;

import auction_server.entities.Item;
import auction_shared.interfaces.SellerAction;

import java.io.Serializable;


public class SellerProfile implements SellerAction, Serializable {
    public void postItem(Item item) {}
}
