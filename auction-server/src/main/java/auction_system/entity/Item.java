package auction_system.entity;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private Seller owner;

    public Item(String name, String description, Seller owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public abstract String getItemCategory();

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Seller getOwner() { return owner; }
}

