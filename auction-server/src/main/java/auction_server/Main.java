package auction_server;

import auction_server.Network.SocketServer;

public class Main {
    public static void main(String[] args){
        new SocketServer().start(8080);
    }
}
