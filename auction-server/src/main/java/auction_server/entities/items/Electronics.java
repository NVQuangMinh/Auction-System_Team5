package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;

public class Electronics extends Item {
    private String brand;

    public Electronics(String name, String description, User owner, String brand) {
        super(name, description, owner);
        this.brand = brand;
    }
    
    public Electronics(String id, String name, String description, User owner, String brand) {
        super(id, name, description, owner);
        this.brand = brand;
    }

    public String getBrand() { return brand; }

    @Override
    public String getDetails() { return "Brand: " + brand; }
}
