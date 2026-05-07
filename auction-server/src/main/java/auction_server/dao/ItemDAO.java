package auction_server.dao;

import auction_server.entities.Item;

public class ItemDAO {

    public Item save(Item item) {
        // TODO: Implement JDBC/JPA logic to save an item.
        // You'll need a way to distinguish item types (e.g., a 'category' column).
        // Based on the category, you'll save to the appropriate table or with the right details.
        // Example: "INSERT INTO items (name, description, owner_id, category, brand, artist_name) VALUES (...)"
        System.out.println("Saving item: " + item.getName());
        return item;
    }
}
