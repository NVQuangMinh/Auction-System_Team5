package auctionshared.dto;

import java.io.Serializable;

public class ItemDTO implements Serializable {

    private String id;
    private String itemName;
    private String description;
    private UserDTO owner;
    private ItemType type;
    private String typeSpecificAttribute;

    public ItemDTO(String id, String itemName, String description,
            UserDTO owner, ItemType type) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.owner = owner;
        this.type = type;
    }

    public ItemDTO(String id, String itemName, String description,
            UserDTO owner, ItemType type, String typeSpecificAttribute) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.owner = owner;
        this.type = type;
        this.typeSpecificAttribute = typeSpecificAttribute;
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

    public ItemType getType() {
        return type;
    }

    public String getTypeSpecificAttribute() {
        return typeSpecificAttribute;
    }

    public String getTypeAttributeLabel() {
        return switch (type) {
            case ARTS -> "Artist";
            case ELECTRONICS -> "Model";
            case VEHICLES -> "Brand";
        };
    }
}
