package auction_server.behaviors;

import auction_server.dao.ItemDAO;
import auction_server.entities.Item;

public class SellerBehaviors implements SellerProfile {

    private static final ItemDAO itemDAO = new ItemDAO();

    @Override
    public void postItem(Item item) {
        itemDAO.insert(item);
    }
}
