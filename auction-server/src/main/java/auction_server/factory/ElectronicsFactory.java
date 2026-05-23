package auction_server.factory;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Electronics;

public class ElectronicsFactory extends ItemFactory<String> {

    @Override
    public Item<String> create(String id, String name, String description, User owner, String typeSpecificAttribute) {
        return new Electronics(id, name, description, owner, typeSpecificAttribute);
    }
}
