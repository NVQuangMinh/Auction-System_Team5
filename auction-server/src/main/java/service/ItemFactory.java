package service;

import auction_client.entity.*;

public class ItemFactory {

    public static Item createItem(String category, String name, String imageUrl, Seller seller, String brand) {
        switch (category.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(name, imageUrl, seller, brand);
            case "ART":
                return new Art(name, imageUrl, seller, brand);
            case "VEHICLE":
                return new Vehicle(name, imageUrl, seller, brand);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ: " + category);
        }
    }
}