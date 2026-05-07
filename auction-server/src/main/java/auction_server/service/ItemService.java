package auction_server.service;

import auction_server.dao.interfaces.ItemDAO;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_shared.dto.CreateItemRequestDTO;

/**
 * Service layer for item-related operations.
 * It orchestrates the creation and persistence of items.
 */
public class ItemService {

    private final ItemDAO itemDAO;
    private final ItemFactory itemFactory;

    // Dependencies are injected via the constructor
    public ItemService(ItemDAO itemDAO, ItemFactory itemFactory) {
        this.itemDAO = itemDAO;
        this.itemFactory = itemFactory;
    }

    /**
     * Orchestrates the entire process of creating and saving an item.
     *
     * @param owner The user creating the item.
     * @param dto   The DTO with the item's data.
     * @return The persisted Item object, now with an ID from the database.
     * @throws Exception if creation or saving fails.
     */
    public Item createAndSaveItem(User owner, CreateItemRequestDTO dto) throws Exception {
        // Step 1: Use the factory to create an in-memory object (ID is null)
        Item newItem = itemFactory.createItem(owner, dto);

        // Step 2: Use the DAO to persist the object to the database
        itemDAO.save(newItem);
        
        // After saving, the newItem object has its ID populated by the DAO.
        // This is crucial for the caller to know the ID of the newly created item.
        return newItem;
    }
}
