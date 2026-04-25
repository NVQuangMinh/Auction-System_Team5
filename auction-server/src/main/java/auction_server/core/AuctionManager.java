package auction_server.core;

import auction_shared.entities.Auction;
import auction_shared.testdata.TestDataProvider; // Import TestDataProvider

import java.util.ArrayList;
import java.util.List; // Import List
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static AuctionManager manager = null;
    private final Map<String, AuctionRoom> activeRooms = new ConcurrentHashMap<>();
//    private AuctionManager(){}
    private AuctionManager(){
        // Load sample data if no auctions are present
        if (activeRooms.isEmpty()) {
            List<Auction> sampleAuctions = TestDataProvider.getSampleAuctions();
            for (Auction auction : sampleAuctions) {
                addRoom(new AuctionRoom(auction)); // Create AuctionRoom from sample Auction
            }
            System.out.println("Loaded " + sampleAuctions.size() + " sample auctions.");
        }
    }

    public static synchronized AuctionManager getInstance(){
        if (manager != null){
            return manager;
        }
        manager = new AuctionManager();
        return manager;
    }
    public void addRoom(AuctionRoom room){
        activeRooms.put(room.getAuction().getItem().getId(),room);
    }
    public AuctionRoom getRoom(String itemId) {
        return activeRooms.get(itemId);
    }

    public ArrayList<Auction> getAllRooms() {
        ArrayList<Auction> rooms = new ArrayList<>();
        for (String i : activeRooms.keySet()){
            rooms.add(activeRooms.get(i).getAuction());
        }
        return rooms;
    }
}
