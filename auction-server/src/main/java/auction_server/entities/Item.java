package auction_server.entities;

import auction_server.base.Entity;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected User owner;

    public Item(String name, String description, User owner) {
        super();
        this.name = name;
        this.description = description;
        this.owner = owner;
    }
    
    public Item(String id, String name, String description, User owner) {
        super(id);
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public User getOwner() { return this.owner; }
    public abstract String getDetails();
}
