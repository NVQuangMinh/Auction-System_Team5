package auction_system.entity;

public class Seller extends User {
    @Override
    public String getRole() {
        return "BIDDER";
    }
}
