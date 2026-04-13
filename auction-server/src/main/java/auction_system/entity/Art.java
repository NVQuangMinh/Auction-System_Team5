package auction_system.entity;

public class Art extends Item {
    public Art(String name, String description, Seller owner) {
        super(name, description, owner);
    }
    @Override
    public String getItemCategory() { return "ART"; }
}
