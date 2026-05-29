package auctionserver.factory;

import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionserver.entities.items.Vehicles;

public class VehiclesFactory extends ItemFactory<String> {

    @Override
    public Item<String> create(String id, String name, String description, User owner, String typeSpecificAttribute) {
        return new Vehicles(id, name, description, owner, typeSpecificAttribute);
    }
}
