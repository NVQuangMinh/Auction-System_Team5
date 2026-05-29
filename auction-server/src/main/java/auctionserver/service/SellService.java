package auctionserver.service;

import auctionserver.dao.AuctionDAO;
import auctionserver.dao.DAOProvider;
import auctionserver.dao.ItemDAO;
import auctionserver.entities.Auction;
import auctionserver.entities.Item;
import auctionserver.exception.DatabaseException;
import auctionserver.exception.TransactionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class SellService {
    private static final Logger log = LoggerFactory.getLogger(SellService.class);
    private final ItemDAO itemDAO;
    private final AuctionDAO auctionDAO;

    public SellService(DAOProvider daoProvider) {
        this.itemDAO = daoProvider.itemDAO();
        this.auctionDAO = daoProvider.auctionDAO();
    }

    public void publishItemAndAuction(Item item, Auction auction) {
        if (item == null || auction == null) {
            throw new IllegalArgumentException("Item and Auction cannot be null");
        }
        if (auction.getStartingPrice() >= auction.getBuyOutPrice()) {
            throw new IllegalArgumentException("Starting price must be less than buy-out price");
        }

        try (Connection conn = auctionDAO.getConnection()) {
            conn.setAutoCommit(false);
            try {
                itemDAO.insert(item, conn);
                auctionDAO.insert(auction, conn);
                conn.commit();
                log.info("Item and auction published successfully: {}", item.getId());
            } catch (SQLException e) {
                conn.rollback();
                log.error("Transaction failed while publishing item and auction: {}", item.getId(), e);
                throw new TransactionFailedException("Failed to publish item and auction: " + item.getId(), e);
            } catch (Exception e) {
                conn.rollback();
                log.error("Unexpected exception occurred while publishing item and auction: {}", item.getId(), e);
                throw new TransactionFailedException("Unexpected error while publishing item and auction: " + item.getId(), e);
            }
        } catch (SQLException e) {
            log.error("Database connection error while publishing item and auction: {}", item.getId(), e);
            throw new DatabaseException("Failed to get database connection", e);
            // thêm catch TransactionFailedException ở đây vì nếu không nó sẽ nhảy vào Exception bên dưới và throw nhầm
        } catch (TransactionFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception occurred while getting database connection for publishing: {}", item.getId(), e);
            throw new DatabaseException("Unexpected error while getting database connection", e);
        }
    }
}
