package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.ItemType;

public class Vehicles extends Item<String> {

    private String brand;


    public Vehicles(String id, String itemName, String description, User owner, String brand) {
        super(id, itemName, description, owner);
        this.brand = brand;
    }

    @Override
    public ItemType getType() {
        return ItemType.VEHICLES;
    }

    @Override
    public String getTypeSpecificAttribute() { return brand; }

    @Override
    public void setTypeSpecificAttribute(String value) { this.brand = value; }

    @Override
    public String getTypeAttributeLabel() { return "Brand"; }
}
