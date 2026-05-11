package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.ItemType;

public class Electronics extends Item {

    public Electronics(String id, String itemName, String description, User owner) {
        super(id, itemName, description, owner);
    }

    @Override
    public ItemType getType() {
        return ItemType.ELECTRONICS;
    }
}
