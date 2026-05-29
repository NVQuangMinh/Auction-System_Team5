package auctionserver.entities.items;

import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionshared.dto.ItemType;

public class Electronics extends Item<String> {

    private String model;

    public Electronics(String id, String itemName, String description, User owner, String model) {
        super(id, itemName, description, owner);
        this.model = model;
    }

    @Override
    public ItemType getType() {
        return ItemType.ELECTRONICS;
    }

    @Override
    public String getTypeSpecificAttribute() {
        return model;
    }

    @Override
    public void setTypeSpecificAttribute(String value) {
        this.model = value;
    }

    @Override
    public String getTypeAttributeLabel() {
        return "Model";
    }
}
