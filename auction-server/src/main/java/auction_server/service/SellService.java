package auction_server.service;

import auction_server.dao.AuctionDAO;
import auction_server.dao.DAOProvider;
import auction_server.dao.ItemDAO;
import auction_server.entities.Auction;
import auction_server.entities.Item;

import java.sql.Connection;
import java.sql.SQLException;

public class SellService {
    private final ItemDAO itemDAO;
    private final AuctionDAO auctionDAO;

    public SellService(DAOProvider daoProvider) {
        this.itemDAO = daoProvider.itemDAO();
        this.auctionDAO = daoProvider.auctionDAO();
    }

    public boolean publishItemAndAuction(Item item, Auction auction) {
        try (Connection conn = auctionDAO.getConnection()) {
            conn.setAutoCommit(false);
            try {
                itemDAO.insert(item, conn);
                auctionDAO.insert(auction, conn);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
