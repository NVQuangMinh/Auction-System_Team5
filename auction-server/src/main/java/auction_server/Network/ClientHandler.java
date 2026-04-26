package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_server.core.AuctionRoom;
import auction_shared.Network.NetworkMessage;
import auction_shared.entities.Auction;
import auction_shared.entities.BidTransaction;
import auction_shared.entities.Item;
import auction_shared.entities.User;
import auction_shared.items.Arts;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable{
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket) { this.socket = socket; }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                handleRequest(msg);
            }
        } catch (Exception e) { /* Xử lý khi Client thoát */ }
    }
    public void sendMessage(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleRequest(NetworkMessage msg) {
        if ("BID".equals(msg.getAction())) {
            BidTransaction bid = (BidTransaction) msg.getData();
            AuctionRoom auctionRoom = AuctionManager.getInstance().getRoom(bid.getAuction().getItem().getId());
            if (auctionRoom != null) {
                if (auctionRoom.placeBid(bid)) {
                    sendMessage(new NetworkMessage("BID_SUCCESS", null));
                    AuctionManager.getInstance().broadCast(new NetworkMessage("UPDATE_BID", null));
                }
                else{
                    sendMessage(new NetworkMessage("BID_FAILED", null));
                }

            }
        }
        else if ("SELL".equals(msg.getAction())){}
        else if ("LOGIN".equals(msg.getAction())){
            /// anh em check database o day sau do gui lai confirmation (isSuccess) cho client nhe!
            sendMessage(new NetworkMessage("LOGIN", (Boolean) true));
            System.out.println(((User) msg.getData()).getUsername() + " successfully login");
        }
        else if ("GET_PRODUCTS".equals(msg.getAction())){
            User user = new User("id","admin", "123456");
            Item item = new Arts("01", "item1", "too good to be true",100, LocalDateTime.now(),user);
            Auction auction = new Auction(item, item.getStartingPrice(), "On Going");
            AuctionRoom room = new AuctionRoom(auction);


            Item item1 = new Arts("02", "item2", "too good to be true",100, LocalDateTime.now(),user);
            Auction auction1 = new Auction(item1, item.getStartingPrice(), "On Going");
            AuctionRoom room1 = new AuctionRoom(auction1);


            Item item2 = new Arts("03", "item3", "too good to be true",100, LocalDateTime.now(),user);
            Auction auction2 = new Auction(item2, item.getStartingPrice(), "On Going");
            AuctionRoom room2 = new AuctionRoom(auction2);


            Item item3 = new Arts("04", "item4", "too good to be true",100, LocalDateTime.now(),user);
            Auction auction3 = new Auction(item3, item.getStartingPrice(), "On Going");
            AuctionRoom room3 = new AuctionRoom(auction3);


            AuctionManager.getInstance().addRoom(room);
            AuctionManager.getInstance().addRoom(room1);
            AuctionManager.getInstance().addRoom(room2);
            AuctionManager.getInstance().addRoom(room3);


            sendMessage(new NetworkMessage("GET_PRODUCTS", AuctionManager.getInstance().getAllRooms()));
        }
    }

}
