package auction_server.factory;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Vehicles;

public class VehiclesFactory extends ItemFactory {

    @Override
    public Item create(String id, String name, String description, User owner) {
        return new Vehicles(id, name, description, owner);
    }
}
