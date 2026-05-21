package auction_server.entities;

import auction_server.base.Entity;
import auction_server.behaviors.AdminProfile;
import auction_server.behaviors.BidderProfile;
import auction_server.behaviors.AdminBehaviors;
import auction_server.behaviors.BidderBehaviors;
import auction_server.behaviors.SellerBehaviors;
import auction_server.behaviors.SellerProfile;

import java.io.Serializable;

public class User extends Entity implements Serializable {
    private String username;
    private String password;
    private String role;
    private String status;

    private BidderProfile bidder = null;
    private SellerProfile seller = null;
    private AdminProfile adminProfile = null;

    public User(String id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = "USER";
        this.status = "AVAILABLE";
        initBehaviors();
    }

    public User(String id, String username, String password, String role, String status) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = status;
        initBehaviors();
    }

    private void initBehaviors() {
        if ("ADMIN".equals(role)) {
            this.adminProfile = new AdminBehaviors();
        } else if ("USER".equals(role)) {
            this.bidder = new BidderBehaviors(this);
            this.seller = new SellerBehaviors();
        }
    }

    public void setBidder(BidderProfile bidder) {
        this.bidder = bidder;
    }

    public void setSeller(SellerProfile seller) {
        this.seller = seller;
    }

    public void performBid(String itemId, double amount) {
        if (bidder != null) bidder.placeBid(itemId, amount);
    }

    public void performPost(Item item) {
        if (seller != null) seller.postItem(item);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
    public String getUserStatus(){
        return status;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
