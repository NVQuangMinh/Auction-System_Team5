package auction_shared.items;

import auction_shared.entities.Item;
import auction_shared.entities.User;

import java.time.LocalDateTime;

public class Electronics extends Item {
    public Electronics(String id, String itemName, String description, double startingPrice, double currentMaxPrice, LocalDateTime endTime, User owner) {
        super(id,itemName,description,startingPrice,currentMaxPrice,endTime,owner);
    }
}
