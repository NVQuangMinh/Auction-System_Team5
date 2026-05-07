package auction_server.behaviors;

import auction_server.entities.Item;

public class SellerProfile {
    private double itemSold;

    public SellerProfile() {
        this.itemSold = 0;
    }

    public void createItem(Item item) {
        // TODO: Implement the logic for creating an item.
        // This might involve saving the item to the database.
    }

    public double getItemSold() {
        return itemSold;
    }
}
