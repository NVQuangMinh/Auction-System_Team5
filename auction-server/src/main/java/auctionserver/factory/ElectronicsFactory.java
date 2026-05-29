package auctionserver.factory;

import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionserver.entities.items.Electronics;

public class ElectronicsFactory extends ItemFactory<String> {

    @Override
    public Item<String> create(String id, String name, String description, User owner, String typeSpecificAttribute) {
        return new Electronics(id, name, description, owner, typeSpecificAttribute);
    }
}
