package auction_server;

import auction_server.Network.SocketServer;
import auction_server.core.AuctionManager;
import auction_server.core.AuctionScheduler;

public class Main {
    public static void main(String[] args) {
        // chạy ngầm Scheduler
        AuctionScheduler scheduler = new AuctionScheduler(AuctionManager.getInstance());
        scheduler.start();
        System.out.println("[System] Auction Scheduler has started.");

        System.out.println("[System] Starting Socket Server...");
        new SocketServer().start(8080);
        System.out.println("Hello World");
    }
}