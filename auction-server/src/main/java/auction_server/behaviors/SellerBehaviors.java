package auction_server.behaviors;

import auction_server.dao.ItemDAO;
import auction_server.entities.Item;

public class SellerBehaviors implements SellerProfile {

    @Override
    public void postItem(Item item) {
        ItemDAO itemDAO = new ItemDAO();
        itemDAO.insert(item);
    }
}
