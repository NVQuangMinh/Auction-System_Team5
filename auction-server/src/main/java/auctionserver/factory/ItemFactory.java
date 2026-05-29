package auctionserver.factory;

import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionshared.dto.ItemType;

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
