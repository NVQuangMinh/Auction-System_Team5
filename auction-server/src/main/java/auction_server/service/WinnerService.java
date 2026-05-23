package auction_server.service;

import auction_server.dao.DAOProvider;
import auction_server.entities.BidTransaction;
import auction_server.entities.User;

import java.util.List;

public class WinnerService {

    public WinnerService(DAOProvider daoProvider) {
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
            if (bidder.getUserStatus() != null && !"BANNED".equals(bidder.getUserStatus())) {
                return bidder.getId();
            }
        }
        return null;
    }
}
