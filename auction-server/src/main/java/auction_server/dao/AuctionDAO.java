package auction_server.dao;

import auction_server.entities.Auction;
import java.util.List;

public class AuctionDAO {

    public Auction findById(Long auctionId) {
        // TODO: Implement JDBC/JPA logic to find an auction by its ID.
        // Example: "SELECT * FROM auctions WHERE id = ?"
        // You will also need to fetch the associated Item and its Owner.
        return null;
    }

    public List<Auction> findAllActive() {
        // TODO: Implement JDBC/JPA logic to find all auctions with "ACTIVE" status.
        return List.of();
    }

    public Auction save(Auction auction) {
        // TODO: Implement JDBC/JPA logic to insert or update an auction.
        // If auction.getId() is null, it's an INSERT. Otherwise, it's an UPDATE.
        // Return the auction with the generated ID if it was an insert.
        System.out.println("Saving auction: " + auction.getItem().getName());
        return auction;
    }
}
