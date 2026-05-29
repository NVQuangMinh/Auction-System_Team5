package auctionserver.behaviors;

import auctionserver.behaviors.profile.SellerProfile;
import auctionserver.dao.ItemDAO;
import auctionserver.entities.Item;

public class SellerBehaviors implements SellerProfile {

    private static final ItemDAO itemDAO = new ItemDAO();

    @Override
    public void postItem(Item item) {
        itemDAO.insert(item);
    }
}
