package auction_server.core;

import auction_server.Network.ClientHandler;
import auction_server.entities.Auction;
import auction_shared.Network.NetworkMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionManager {
    private static AuctionManager manager = null;
    private final Map<String, Auction> activeRooms = new ConcurrentHashMap<>();
    private static List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    private AuctionManager(){}
    public static synchronized AuctionManager getInstance(){
        if (manager != null){
            return manager;
        }
        manager = new AuctionManager();
        return manager;
    }
    public void addRoom(Auction auction){
        activeRooms.put(auction.getItem().getId(), auction);
    }
    public Auction getRoom(String itemId) {
        return activeRooms.get(itemId);
    }

    public ArrayList<Auction> getAllRooms() {
        ArrayList<Auction> rooms = new ArrayList<>();
        for (String i : activeRooms.keySet()){
            rooms.add(activeRooms.get(i));
        }
        return rooms;
    }

    public static void addClient(ClientHandler client) {
        activeClients.add(client);
    }

    public static void removeClient(ClientHandler client) {
        activeClients.remove(client);
    }

    public void broadCast(NetworkMessage msg){
        for (ClientHandler client : activeClients){
            client.sendMessage(msg);
        }
    }

    public void removeRoom(Auction room){
        activeRooms.remove(room.getItem().getId());
    }
}
