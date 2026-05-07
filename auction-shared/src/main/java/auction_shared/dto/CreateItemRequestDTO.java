package auction_shared.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for sending a request from the client to the server to create a new item.
 */
public class CreateItemRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String category; // "art", "vehicle", "electronics"
    private String name;
    private String description;
    
    // Using a Map to hold specific details: e.g., {"artistName": "Da Vinci"}, {"brand": "Honda"}
    private Map<String, String> specificDetails;

    public CreateItemRequestDTO(String category, String name, String description, Map<String, String> specificDetails) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.specificDetails = specificDetails;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getSpecificDetails() {
        return specificDetails;
    }
}
