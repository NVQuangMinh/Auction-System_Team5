package entity;

import auction_client.entity.Entity;

public abstract class User extends Entity {
    private String username;
    private String passwordHash;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public abstract String getRole();

    public boolean login(String inputPassword) {
        //TODO: nối với database
        return this.passwordHash.equals(inputPassword);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}