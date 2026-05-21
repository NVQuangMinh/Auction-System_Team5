package auction_server.dao;

public interface DAOProvider {
    AuctionDAO auctionDAO();
    BidTransactionDAO bidTransactionDAO();
    ItemDAO itemDAO();
    UserDAO userDAO();
}
