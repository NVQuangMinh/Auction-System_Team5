package auction_server.factory;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicles;
import auction_shared.dto.ItemType;

public class ItemFactory {

    private ItemFactory() {
    }

    public static Item create(ItemType type, String id, String name,
            String description, User owner) {
        return switch (type) {
            case ARTS -> new Arts(id, name, description, owner);
            case ELECTRONICS -> new Electronics(id, name, description, owner);
            case VEHICLES -> new Vehicles(id, name, description, owner);
        };
    }
}
