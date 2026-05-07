package auction_server.dao;

import auction_server.entities.BidTransaction;

public class BidTransactionDAO {

    public BidTransaction save(BidTransaction transaction) {
        // TODO: Implement JDBC/JPA logic to save a bid transaction.
        // Example: "INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp) VALUES (...)"
        System.out.println("Saving transaction for auction " + transaction.getAuction().getId() + " with amount " + transaction.getBidAmount());
        return transaction;
    }
}
