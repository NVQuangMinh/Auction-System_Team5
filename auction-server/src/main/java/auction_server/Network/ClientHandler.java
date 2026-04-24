package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_shared.Network.NetworkMessage;
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

    private void handleRequest(NetworkMessage msg) {
        if ("BID".equals(msg.getAction())) {}
        else if ("SELL".equals(msg.getAction())){}
        else if ("LOGIN".equals(msg.getAction())){
            // anh em check database o day sau do gui lai confirmation cho client nhe!
            System.out.println(((User) msg.getData()).getUsername() + " successfully login");
        }
        else if ("GET_PRODUCTS".equals(msg.getAction())){
            try{
                out.writeObject(new NetworkMessage("GET_PRODUCTS",AuctionManager.getInstance().getAllRooms()));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
