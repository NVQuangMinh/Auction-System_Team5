package auction_server.factory;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.ItemType;

public abstract class ItemFactory<T> {

    public abstract Item<T> create(String id, String name, String description, User owner, T typeSpecificAttribute);

    public static ItemFactory<String> of(ItemType type) {
        return switch (type) {
            case ARTS -> new ArtsFactory();
            case ELECTRONICS -> new ElectronicsFactory();
            case VEHICLES -> new VehiclesFactory();
        };
    }
}
