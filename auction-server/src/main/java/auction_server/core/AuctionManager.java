package auction_server.core;

import auction_shared.entities.Auction;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static AuctionManager manager = null;
    private final Map<String, AuctionRoom> activeRooms = new ConcurrentHashMap<>();
    private AuctionManager(){}
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
