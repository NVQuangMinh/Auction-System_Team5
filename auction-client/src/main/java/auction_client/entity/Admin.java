package auction_client.entity;

public class Admin extends User {
    public Admin(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
    }

    @Override
    public String getRole() { return "ADMIN"; }
}