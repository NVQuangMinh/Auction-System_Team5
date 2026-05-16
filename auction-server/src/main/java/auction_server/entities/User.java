package auction_server.entities;

import auction_server.base.Entity;
import auction_server.behaviors.AdminProfile;
import auction_server.interfaces.BidderAction;
import auction_server.interfaces.SellerAction;

import java.io.Serializable;

public class User extends Entity implements Serializable {
    protected String username;
    protected String password;
    protected String role;
    protected String status;
    protected BidderAction bidder = null;
    protected SellerAction seller = null;
    protected AdminProfile adminProfile = null;

    public User(String id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = "USER";
        this.status = "AVAILABLE";
    }

    public User(String id, String username, String password, String role, String status) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    public void setBidder(BidderAction bidder) {
        this.bidder = bidder;
    }

    public void setSeller(SellerAction seller) {
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
