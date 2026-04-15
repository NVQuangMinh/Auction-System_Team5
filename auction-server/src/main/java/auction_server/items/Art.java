package auction_server.items;

import auction_server.entities.Item;

import java.time.LocalDateTime;

public class Art extends Item {
    public Art(String id, String itemName, String description, double startingPrice, double currentMaxPrice, LocalDateTime endTime) {
        super(id,itemName,description,startingPrice,currentMaxPrice,endTime);
    }
}
