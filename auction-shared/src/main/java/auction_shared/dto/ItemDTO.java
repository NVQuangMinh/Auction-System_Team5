package auction_shared.dto;

import java.io.Serializable;

public class ItemDTO implements Serializable {
    private String id;
    private String itemName;
    private String description;
    private UserDTO owner;

    public ItemDTO(String id, String itemName, String description, UserDTO owner) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public UserDTO getOwner() {
        return owner;
    }
}
