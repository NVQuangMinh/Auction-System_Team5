package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;

import java.time.LocalDateTime;

public class Vehicles extends Item {
    public Vehicles(String id, String itemName, String description, User owner) {
        super(id, itemName, description, owner);
    }
}
