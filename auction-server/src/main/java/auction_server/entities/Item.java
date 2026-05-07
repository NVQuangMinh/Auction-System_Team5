package auction_server.entities;

import auction_server.base.Entity;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {
    protected String name;
    protected String description;
    protected User owner;

    public Item(String id, String name, String description, User owner) {
        super(id);
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public User getOwner() {
        return this.owner;
    }

    /**
     * Returns specific details for the item subclass.
     * @return A formatted string with item details.
     */
    public abstract String getDetails();
}
