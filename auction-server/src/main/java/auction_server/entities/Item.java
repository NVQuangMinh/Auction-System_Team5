package auction_server.entities;

import java.io.Serializable;

import auction_server.base.Entity;
import auction_shared.dto.ItemType;

public abstract class Item<T> extends Entity implements Serializable {

    protected String itemName;
    protected String description;
    protected User owner;

    public Item(String id, String itemName, String description, User owner) {
        super(id);
        this.itemName = itemName;
        this.description = description;
        this.owner = owner;
    }

    public abstract ItemType getType();

    public abstract T getTypeSpecificAttribute();
    public abstract void setTypeSpecificAttribute(T value);

    public abstract String getTypeAttributeLabel();

    public String getName() {
        return this.itemName;
    }

    public String getDescription() {
        return this.description;
    }

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User newOwner) {
        this.owner = newOwner;
    }
}
