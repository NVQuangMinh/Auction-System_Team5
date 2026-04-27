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
import java.io.Serializable;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        String action = msg.getAction();
        if ("PLACE_BID".equals(action)) {
            BidTransaction transaction = (BidTransaction) msg.getData();
            AuctionRoom auctionRoom = AuctionManager.getInstance().getRoom(transaction.getAuction().getItem().getId());
            if (auctionRoom != null) {
                if (auctionRoom.placeBid(transaction)) {
                    sendMessage(new NetworkMessage("BID_SUCCESS", null));
                    AuctionManager.getInstance().broadCast(new NetworkMessage("UPDATE_BID", null));
                }
                else{
                    sendMessage(new NetworkMessage("BID_FAILED", null));
                }
            }
        }
        else if ("SELL".equals(action)){}
        else if ("LOGIN".equals(action)){
            /// anh em check database o day sau do gui lai confirmation (isSuccess) cho client nhe!
            sendMessage(new NetworkMessage("LOGIN", (Boolean) true));
            System.out.println(((User) msg.getData()).getUsername() + " successfully login");
        }
        else if ("GET_PRODUCTS".equals(action)){
            User user1 = new User("id","vuminh","123456");
            Item item1 = new Arts("01","MONA_LISA","A beautiful girl",user1);
            Auction auction1 = new Auction(item1, 100, 1000,10, LocalDateTime.now(),LocalDateTime.now().plusMinutes(5));
            AuctionRoom room1 = new AuctionRoom(auction1);
            AuctionManager.getInstance().addRoom(room1);
            sendMessage(new NetworkMessage("GET_PRODUCTS", AuctionManager.getInstance().getAllRooms()));
        }
        else if ("BUY_OUT".equals(action)){
            BidTransaction transaction = (BidTransaction) msg.getData();
            AuctionRoom auctionRoom = AuctionManager.getInstance().getRoom(transaction.getAuction().getItem().getId());
            if (auctionRoom.buyOut(transaction)){
                sendMessage(new NetworkMessage("BUYOUT_SUCCESS", null));
                AuctionManager.getInstance().broadCast(new NetworkMessage("UPDATE_BID",null));
            }
        }
        else if ("GET_MY_LIST".equals(action)){
            List<Auction> myList = new ArrayList<>();
            String userName = (String) msg.getData();
            for (Auction auction : AuctionManager.getInstance().getAllRooms()){
                if (userName.equals(auction.getItem().getOwner().getUsername())){
                    myList.add(auction);
                }
            }
            sendMessage(new NetworkMessage("GET_MY_LIST",(Serializable) myList));
        }
        else if ("CREATE_ACCOUNT".equals(action)){
            //Check database to see if this user existed
            //Send isSuccess to confirm the action
            //that's all (:
        }
    }

}
