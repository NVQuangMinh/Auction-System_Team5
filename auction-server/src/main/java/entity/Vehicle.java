package entity;

public class Vehicle extends Item {
    private String brand;

    public Vehicle(String name, String imageUrl, Seller seller, String brand) {
        super(name, imageUrl, seller);
        this.brand = brand;
    }

    @Override
    public String getItemCategory() {
        return "VEHICLE";
    }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}