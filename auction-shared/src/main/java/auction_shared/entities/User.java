package auction_shared.entities;
import auction_shared.base.Entity;
import auction_shared.interfaces.BidderAction;
import auction_shared.interfaces.SellerAction;

public class User extends Entity {
    protected String username;
    protected String password;
    protected BidderAction bidder = null;
    protected SellerAction seller = null;
    public User(String id,String username, String password){
        super(id);
        this.username = username;
        this.password = password;
    }
    public void setBidder(BidderAction bidder){this.bidder = bidder;}
    public void setSeller(SellerAction seller){this.seller = seller;}

    public void performBid(String itemId, double amount) {
        if (bidder != null) bidder.placeBid(itemId, amount);
    }

    public void performPost(Item item) {
        if (seller != null) seller.postItem(item);
    }

    public String getUsername() {return username;}
}
