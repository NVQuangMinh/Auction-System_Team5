package auction_system.entity;

public class Seller extends User {
    public Seller(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }
}
