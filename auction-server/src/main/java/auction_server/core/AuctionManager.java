package auction_server.core;

import auction_server.Network.ClientHandler;
import auction_server.entities.Auction;
import auction_shared.Network.NetworkMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {
    private static volatile AuctionManager manager = null;
    private final Map<String, Auction> activeRooms = new ConcurrentHashMap<>();
    private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private AuctionManager() {
    }

    public static synchronized AuctionManager getInstance() {
        if (manager != null) {
            return manager;
        }
        manager = new AuctionManager();
        return manager;
    }

    public void addRoom(Auction auction) {
        activeRooms.put(auction.getItem().getId(), auction);
    }

    public Auction getRoom(String itemId) {
        return activeRooms.get(itemId);
    }

    public ArrayList<Auction> getAllRooms() {
        return new ArrayList<>(activeRooms.values());
    }

    public void addClient(ClientHandler client) {
        activeClients.add(client);
    }

    public List<ClientHandler> getActiveClients() {
        return activeClients;
    }

    public void removeClient(ClientHandler client) {
        activeClients.remove(client);
    }

    public void broadCast(NetworkMessage msg) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(msg);
        }
    }

    public void removeRoom(Auction room) {
        lock.lock();
        try {
            activeRooms.remove(room.getItem().getId());
        } finally {
            lock.unlock();
        }

    }
}
