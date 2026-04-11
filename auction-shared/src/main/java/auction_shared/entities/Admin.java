package auction_shared.entities;


public class Admin extends User {
    public Admin(String id, String username, String password) {
        super(id, username, password);
        this.setBidder(null);
        this.setSeller(null);
    }
}
