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
            throw new IllegalArgumentException("Sản phẩm và phiên đấu giá không được trống");
        }
        if (auction.getStartingPrice() >= auction.getBuyOutPrice()) {
            throw new IllegalArgumentException("Giá khởi điểm phải nhỏ hơn giá mua");
        }

        try (Connection conn = auctionDAO.getConnection()) {
            conn.setAutoCommit(false);
            try {
                itemDAO.insert(item, conn);
                auctionDAO.insert(auction, conn);
                conn.commit();
                log.info("Đã đăng sản phẩm và phiên đấu giá thành công : {}", item.getId());
            } catch (SQLException e) {
                conn.rollback();
                log.error("`Thất bại khi đăng sản phẩm và phiên đấu giá`: {}", item.getId(), e);
                throw new TransactionFailedException("Thất bại khi đăng sản phẩm và phiên đấu giá: " + item.getId(), e);
            } catch (Exception e) {
                conn.rollback();
                log.error("Đã xảy ra lỗi khi đăng sản phẩm và phiên đấu giá: {}", item.getId(), e);
                throw new TransactionFailedException("Đã xảy ra lỗi khi đăng sản phẩm và phiên đấu giá: " + item.getId(), e);
            }
        } catch (SQLException e) {
            log.error("Lỗi cơ sở dữ liệu khi đăng sản phẩm và phiên đấu giá: {}", item.getId(), e);
            throw new DatabaseException("Không thể kết nối với cơ sở dữ liệu", e);
            // thêm catch TransactionFailedException ở đây vì nếu không nó sẽ nhảy vào Exception bên dưới và throw nhầm
        } catch (TransactionFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi khi kết nối cơ sở dữ liệu: {}", item.getId(), e);
            throw new DatabaseException("Đã xảy ra lỗi khi kết nối cơ sở dữ liệu", e);
        }
    }
}
