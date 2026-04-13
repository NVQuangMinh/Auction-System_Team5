package auction_system.entity;

public class Vehicle extends Item {
    public Vehicle(String name, String description, Seller owner) {
        super(name, description, owner);
    }
    @Override
    public String getItemCategory() { return "VEHICLE"; }
}
