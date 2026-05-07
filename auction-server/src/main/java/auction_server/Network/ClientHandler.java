package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.entities.items.Arts;
import auction_server.mapper.Mappers;
import auction_server.service.UserService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.BidTransactionDTO;
import auction_shared.dto.SignUpDTO;
import auction_shared.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final UserService userService = new UserService();

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
            log.info("fail to send message",e);
        }
    }
    private void handleRequest(NetworkMessage msg) {
        String action = msg.getAction();
        if ("PLACE_BID".equals(action)) {
            BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();
            // create a user by searching in database using transactionDTO.getBidder().getId()
            // and then when creating transaction, replace null = user.
            BidTransaction transaction = new BidTransaction(
                AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId()),
                null,
                transactionDTO.getBidAmount()
            );
            Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
            if (auction != null) {
                if (auction.placeBid(transaction)) {
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
            SignUpDTO dto = (SignUpDTO) msg.getData();
            User user = userService.login(dto.getUsername(), dto.getPassword());
            boolean isSuccess = user != null;
            sendMessage(new NetworkMessage("LOGIN", isSuccess));
            log.info(dto.getUsername() + (isSuccess ? " successfully login" : " failed to login"));
        }
        else if ("GET_PRODUCTS".equals(action)){
            User user1 = new User("id","vuminh","123456");
            Item item1 = new Arts("01","MONA_LISA","A beautiful girl",user1);
            Auction auction1 = new Auction(item1, 100, 1000,10, LocalDateTime.now(),LocalDateTime.now().plusMinutes(5));
            AuctionManager.getInstance().addRoom(auction1);
            sendMessage(new NetworkMessage("GET_PRODUCTS", (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())));
        }
        else if ("BUY_OUT".equals(action)){
            BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();
            // create a user by searching in database using transactionDTO.getBidder().getId()
            // and then when creating transaction, replace null = user.
            BidTransaction transaction = new BidTransaction(
                AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId()),
                null,
                transactionDTO.getBidAmount()
            );
            Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
            if (auction.buyOut(transaction)){
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
            sendMessage(new NetworkMessage("GET_MY_LIST",(Serializable) Mappers.toAuctionDTOList(myList)));
        }
        else if ("CREATE_ACCOUNT".equals(action)){
            SignUpDTO dto = (SignUpDTO) msg.getData();
            User newUser = new User(dto.getId(), dto.getUsername(), dto.getPassword());
            boolean isSuccess = userService.register(newUser);
            sendMessage(new NetworkMessage("CREATE_ACCOUNT", isSuccess));
        }
    }

}
