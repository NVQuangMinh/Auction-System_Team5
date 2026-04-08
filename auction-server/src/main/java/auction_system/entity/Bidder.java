package auction_system.entity;

public class Bidder extends User {
    @Override
    public String getRole() {
        return "BIDDER";
    }
}
