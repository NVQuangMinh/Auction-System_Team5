package auction_server.factory;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Arts;

public class ArtsFactory extends ItemFactory<String> {

    @Override
    public Item<String> create(String id, String name, String description, User owner, String typeSpecificAttribute) {
        return new Arts(id, name, description, owner, typeSpecificAttribute);
    }
}
