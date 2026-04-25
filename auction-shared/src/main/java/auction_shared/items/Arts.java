package auction_shared.items;

import auction_shared.entities.Item;
import auction_shared.entities.User;

import java.time.LocalDateTime;

public class Arts extends Item {
    public Arts(String id, String itemName, String description, double startingPrice, double buyOutPrice, double productTickRate, LocalDateTime endTime, User owner) {
        super(id, itemName, description, startingPrice, buyOutPrice, productTickRate, endTime, owner);
    }
}
