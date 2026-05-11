package auction_server.factory;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Electronics;

public class ElectronicsFactory extends ItemFactory {

    @Override
    public Item create(String id, String name, String description, User owner) {
        return new Electronics(id, name, description, owner);
    }
}
