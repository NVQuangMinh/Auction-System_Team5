package auction_server.service;

import auction_server.dao.UserDAO;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;

import java.util.List;

public class WinnerService {

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
            User user = UserDAO.getUserByUsername(bidder.getUsername());
            if (user != null && !"BANNED".equals(user.getUserStatus())) {
                return bidder.getId();
            }
        }
        return null;
    }
}
