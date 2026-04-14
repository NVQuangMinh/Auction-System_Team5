package auction_client.entity;

public class Art extends Item {
    private String brand;

    public Art(String name, String imageUrl, Seller seller, String brand) {
        super(name, imageUrl, seller);
        this.brand = brand;
    }

    @Override
    public String getItemCategory() {
        return "ART";
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}
