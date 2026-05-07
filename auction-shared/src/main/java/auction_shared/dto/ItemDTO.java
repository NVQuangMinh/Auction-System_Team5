package auction_shared.dto;

import java.io.Serializable;

public class ItemDTO implements Serializable {
    private String id;
    private String itemName;
    private String description;
    private UserDTO owner;
    private String details; // Bổ sung trường này

    // Cập nhật constructor để nhận 5 tham số
    public ItemDTO(String id, String itemName, String description, UserDTO owner, String details) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.owner = owner;
        this.details = details;
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

    public String getDetails() {
        return details;
    }
}