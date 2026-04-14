package entity;

import auction_client.entity.Auction;
import auction_client.entity.User;

public class Bidder extends User {

    public Bidder(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    public void placeBid(Auction auction, double amount) {
        // TODO: Ở Client, hàm này tạo Socket Request. Ở Server, đẩy Request vào service.AuctionService để xử lý qua ReentrantLock.
    }

}