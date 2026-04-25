package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_server.core.AuctionRoom;
import auction_shared.Network.NetworkMessage;
import auction_shared.entities.BidTransaction;
import auction_shared.entities.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
                    AuctionManager.getInstance().broadCast(new NetworkMessage("UPDATE_PRICE", null));
                }
                else{
                    sendMessage(new NetworkMessage("BID_FAILED", null));
                }

            }
        }
        else if ("SELL".equals(msg.getAction())){}
        else if ("LOGIN".equals(msg.getAction())){
            // anh em check database o day sau do gui lai confirmation cho client nhe!
            System.out.println(((User) msg.getData()).getUsername() + " successfully login");
        }
        else if ("GET_PRODUCTS".equals(msg.getAction())){
            sendMessage(new NetworkMessage("GET_PRODUCTS", AuctionManager.getInstance().getAllRooms()));
        }
    }

}
