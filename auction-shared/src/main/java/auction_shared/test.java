package auction_shared;

import auction_shared.behaviors.StandardBidder;
import auction_shared.entities.User;

public class test {
    public static void main(String[] args) {
        User user = new User("#01","vu-minh", "123456");

        user.setBidder(new StandardBidder());

        System.out.println("check name: " + user.getUsername());
        user.performBid("item #01", 500.0);
    }
}
