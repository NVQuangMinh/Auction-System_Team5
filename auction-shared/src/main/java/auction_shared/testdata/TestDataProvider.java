package auction_shared.testdata;

import auction_shared.entities.Auction;
import auction_shared.entities.User;
import auction_shared.items.Electronics;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestDataProvider {
    public static List<Auction> getSampleAuctions() {
        List<Auction> list = new ArrayList<>();
        User owner = new User("1", "Admin", "123");
        
        // Item 1
        Electronics laptop = new Electronics("item1", "Laptop Dell XPS", "High-end laptop", 1000, 2000,
                50, LocalDateTime.now().plusDays(1), owner);
        list.add(new Auction(laptop, 1100, "ACTIVE"));
        
        // Item 2
        Electronics phone = new Electronics("item2", "iPhone 15 Pro", "Newest iPhone", 900, 1500,
                20, LocalDateTime.now().plusHours(5), owner);
        list.add(new Auction(phone, 920, "ACTIVE"));
        
        return list;
    }
}
