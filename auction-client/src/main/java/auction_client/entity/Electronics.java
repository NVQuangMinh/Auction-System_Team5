package auction_client.entity;

public class Electronics extends Item {
    public Electronics(String name, String description, Seller owner) {
        super(name, description, owner);
    }
    @Override
    public String getItemCategory() { return "ELECTRONICS"; }
}
