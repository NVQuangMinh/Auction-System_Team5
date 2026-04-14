package entity;

import auction_client.entity.Entity;
import auction_client.entity.Seller;

public abstract class Item extends Entity {
    private String name;
    private String imageUrl;
    private Seller seller;

    public Item(String name, String imageUrl, Seller seller) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.seller = seller;
    }

    public abstract String getItemCategory();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }
}