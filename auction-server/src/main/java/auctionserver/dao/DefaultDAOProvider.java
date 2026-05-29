package auctionserver.dao;

public class DefaultDAOProvider implements DAOProvider {
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    public AuctionDAO auctionDAO() {
        return auctionDAO;
    }

    @Override
    public BidTransactionDAO bidTransactionDAO() {
        return bidTransactionDAO;
    }

    @Override
    public ItemDAO itemDAO() {
        return itemDAO;
    }

    @Override
    public UserDAO userDAO() {
        return userDAO;
    }
}
