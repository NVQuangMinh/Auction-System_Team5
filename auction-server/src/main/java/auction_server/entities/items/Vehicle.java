package auction_server.entities.items;

import auction_server.entities.Item;
import auction_server.entities.User;

public class Vehicle extends Item {
    private String brand;

    public Vehicle(Long id, String name, String description, User owner, String brand) {
        super(id, name, description, owner);
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    @Override
    public String getDetails() {
        return "Brand: " + brand;
    }
}
