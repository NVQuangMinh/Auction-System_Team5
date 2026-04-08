package auction_system.entity;

public abstract class User extends Entity {
    private String username;
    private String passwordHash;

    public abstract String getRole();

    // Getters, Setters
}
