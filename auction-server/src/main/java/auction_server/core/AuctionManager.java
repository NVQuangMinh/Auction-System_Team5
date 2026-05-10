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
    private static volatile AuctionManager instance;
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    public Auction getAuction(Long auctionId) {
        return activeAuctions.get(auctionId);
    }

    public void removeAuction(Long auctionId) {
        activeAuctions.remove(auctionId);
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public void addClient(ClientHandler client) {
        activeClients.add(client);
    }

    public void removeClient(ClientHandler client) {
        activeClients.remove(client);
    }

    public void broadCast(NetworkMessage msg) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(msg);
        }
    }

    public void removeRoom(Auction room){
        activeRooms.remove(room.getItem().getId());
    }
}
