package auction_server.entities;

import auction_server.base.Entity;
import auction_server.interfaces.CanBid;
import auction_server.interfaces.CanSell;

public class User extends Entity {
    protected String username;
    protected String password;
    protected CanBid bidder = null;
    protected CanSell seller = null;
    public User(String id,String username, String password){
        super(id);
        this.username = username;
        this.password = password;
    }
    public void setBidder(CanBid bidder){this.bidder = bidder;}
    public void setSeller(CanSell seller){this.seller = seller;}

    public void performBid(String itemId, double amount) {
        if (bidder != null) bidder.placeBid(itemId, amount);
    }

    public void performPost(Item item) {
        if (seller != null) seller.postItem(item);
    }

    public String getUsername() {return username;}
}

