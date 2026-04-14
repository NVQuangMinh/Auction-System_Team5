package entity;

import auction_client.entity.Item;
import auction_client.entity.Seller;

public class Electronics extends Item {
    private String brand;

    public Electronics(String name, String imageUrl, Seller seller, String brand) {
        super(name, imageUrl, seller);
        this.brand = brand;
    }

    @Override
    public String getItemCategory() {
        return "ELECTRONICS";
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}