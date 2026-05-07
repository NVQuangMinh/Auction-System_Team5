package auction_server.entities;

import auction_server.base.Entity;
import auction_server.behaviors.AdminProfile;
import auction_server.behaviors.BidderProfile;
import auction_server.behaviors.SellerProfile;

import java.util.ArrayList;
import java.util.List;

public class User extends Entity {
    private String username;
    private String passwordHash;

    private BidderProfile bidderProfile;
    private SellerProfile sellerProfile;
    private AdminProfile adminProfile;

    public User(Long id, String username, String passwordHash) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public boolean login(String password) {
        // TODO: Implement proper password hashing and comparison.
        // This is a placeholder and is NOT secure.
        return this.passwordHash.equals(password);
    }

    public boolean hasRole(Class<?> roleType) {
        if (roleType == BidderProfile.class && bidderProfile != null) {
            return true;
        }
        if (roleType == SellerProfile.class && sellerProfile != null) {
            return true;
        }
        if (roleType == AdminProfile.class && adminProfile != null) {
            return true;
        }
        return false;
    }

    public List<Object> getActiveProfiles() {
        List<Object> profiles = new ArrayList<>();
        if (bidderProfile != null) {
            profiles.add(bidderProfile);
        }
        if (sellerProfile != null) {
            profiles.add(sellerProfile);
        }
        if (adminProfile != null) {
            profiles.add(adminProfile);
        }
        return profiles;
    }

    // Getters and Setters for profiles
    public BidderProfile getBidderProfile() {
        return bidderProfile;
    }

    public void setBidderProfile(BidderProfile bidderProfile) {
        this.bidderProfile = bidderProfile;
    }

    public SellerProfile getSellerProfile() {
        return sellerProfile;
    }

    public void setSellerProfile(SellerProfile sellerProfile) {
        this.sellerProfile = sellerProfile;
    }

    public AdminProfile getAdminProfile() {
        return adminProfile;
    }

    public void setAdminProfile(AdminProfile adminProfile) {
        this.adminProfile = adminProfile;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
