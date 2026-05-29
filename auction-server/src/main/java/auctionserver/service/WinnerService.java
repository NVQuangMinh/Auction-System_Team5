package auctionserver.service;

import auctionserver.dao.DAOProvider;
import auctionserver.dao.UserDAO;
import auctionserver.entities.BidTransaction;
import auctionserver.entities.User;

import java.util.List;

public class WinnerService {

    private final UserDAO userDAO;

    public WinnerService(DAOProvider daoProvider) {
        if (daoProvider == null) {
            throw new IllegalArgumentException("DAOProvider cannot be null");
        }
        this.userDAO = daoProvider.userDAO();
    }

    public String determineWinner(List<BidTransaction> bidHistory) {
        if (bidHistory == null || bidHistory.isEmpty()) {
            return null;
        }
        for (int i = bidHistory.size() - 1; i >= 0; i--) {
            BidTransaction tx = bidHistory.get(i);
            User bidder = tx.getBidder();
            if (bidder == null) {
                continue;
            }
            User latestUser = userDAO.getUserByUsername(bidder.getUsername());
            if (latestUser != null && !"BANNED".equals(latestUser.getUserStatus())) {
                return bidder.getId();
            }
        }
        return null;
    }
}
