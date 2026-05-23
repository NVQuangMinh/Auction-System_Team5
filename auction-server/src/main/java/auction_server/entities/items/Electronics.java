package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.ItemType;

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
    public String getTypeSpecificAttribute() { return model; }

    @Override
    public void setTypeSpecificAttribute(String value) { this.model = value; }

    @Override
    public String getTypeAttributeLabel() { return "Model"; }
}
