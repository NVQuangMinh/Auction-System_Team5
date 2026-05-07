package auction_server.service;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Art;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicle;

public class ItemFactory {
    public static Item createItem(String category, Long id, String name, String desc, User owner, Object... params) {
        // TODO: Validate that the user has a SellerProfile.
        // if (!owner.hasRole(SellerProfile.class)) {
        //     throw new IllegalArgumentException("User must be a seller to create items.");
        // }

        switch (category.toLowerCase()) {
            case "art":
                if (params.length > 0 && params[0] instanceof String) {
                    return new Art(id, name, desc, owner, (String) params[0]);
                }
                throw new IllegalArgumentException("Art requires an artist name (String).");
            case "electronics":
                if (params.length > 0 && params[0] instanceof String) {
                    return new Electronics(id, name, desc, owner, (String) params[0]);
                }
                throw new IllegalArgumentException("Electronics requires a brand (String).");
            case "vehicle":
                if (params.length > 0 && params[0] instanceof String) {
                    return new Vehicle(id, name, desc, owner, (String) params[0]);
                }
                throw new IllegalArgumentException("Vehicle requires a brand (String).");
            default:
                throw new IllegalArgumentException("Unknown item category: " + category);
        }
    }
}
