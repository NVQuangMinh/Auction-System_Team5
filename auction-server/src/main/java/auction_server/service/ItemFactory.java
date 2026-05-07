package auction_server.service;

import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Art;
import auction_server.entities.items.Electronics;
import auction_server.entities.items.Vehicle;
import auction_shared.dto.CreateItemRequestDTO;

import java.util.Map;

/**
 * A factory for creating Item entity objects.
 * This is a regular component, not a static utility class, to allow for future dependency injection.
 */
public class ItemFactory {

    /**
     * Creates an in-memory Item object from a DTO.
     * The ID of the returned object is null, as it has not been persisted yet.
     *
     * @param owner The user who owns the item. Must have a SellerProfile.
     * @param dto   The data transfer object containing item details.
     * @return A new, non-persisted Item object.
     * @throws Exception if validation fails.
     */
    public Item createItem(User owner, CreateItemRequestDTO dto) throws Exception {
        // Business logic validation: Ensure the user is a seller.
        if (owner.getSellerProfile() == null) {
            throw new IllegalStateException("User must have a Seller Profile to create items.");
        }

        String category = dto.getCategory().toLowerCase();
        String name = dto.getName();
        String description = dto.getDescription();
        Map<String, String> details = dto.getSpecificDetails();

        switch (category) {
            case "art":
                String artistName = details.get("artistName");
                if (artistName == null || artistName.isBlank()) {
                    throw new IllegalArgumentException("Art requires an 'artistName' in specificDetails.");
                }
                // ID is null because the database will generate it.
                return new Art(null, name, description, owner, artistName);

            case "electronics":
                String brandE = details.get("brand");
                if (brandE == null || brandE.isBlank()) {
                    throw new IllegalArgumentException("Electronics requires a 'brand' in specificDetails.");
                }
                return new Electronics(null, name, description, owner, brandE);

            case "vehicle":
                String brandV = details.get("brand");
                if (brandV == null || brandV.isBlank()) {
                    throw new IllegalArgumentException("Vehicle requires a 'brand' in specificDetails.");
                }
                return new Vehicle(null, name, description, owner, brandV);

            default:
                throw new IllegalArgumentException("Unknown item category: " + category);
        }
    }
}
