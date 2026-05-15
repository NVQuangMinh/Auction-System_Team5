package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_server.dao.AuctionDAO;
import auction_server.dao.ItemDAO;
import auction_server.dao.UserDAO;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.factory.ItemFactory;
import auction_server.mapper.Mappers;
import auction_server.service.UserService;
import auction_shared.Network.NetworkMessage;
import auction_shared.Network.Notification;
import auction_shared.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private List<Notification> activities = new ArrayList<>();

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final UserService userService = new UserService();


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                try {
                    handleRequest(msg);
                } catch (Exception e) {
                    log.error("Error handling request: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.info("Client has disconnected");
        }
    }

    public void sendMessage(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            log.info("fail to send message", e);
        }
    }

    private void handleRequest(NetworkMessage msg) {
        String action = msg.getAction();
        log.info("Handling request: {}, current activities size: {}", action, activities.size());
        if ("PLACE_BID".equals(action)) {
            BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();
            User bidder = UserDAO.getUserByUsername(transactionDTO.getBidder().getUsername());
            BidTransaction transaction = new BidTransaction(
                    AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId()),
                    bidder,
                    transactionDTO.getBidAmount());
            Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
            if (auction != null) {
                if (auction.placeBid(transaction)) {
                    sendMessage(new NetworkMessage("BID_SUCCESS", Mappers.toDTO(auction)));
                    AuctionManager.getInstance().broadCast(new NetworkMessage(
                            "UPDATE_BID",
                            (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())
                    ));
                    log.info("A new bid has been placed");
                    activities.add(new Notification("you have placed bid successfully", LocalTime.now()));
                } else {
                    log.info("Your bid has failed");
                    sendMessage(new NetworkMessage("BID_FAILED", null));
                    activities.add(new Notification("Your bid has failed", LocalTime.now()));
                }
            }
        } else if ("SELL".equals(action)) {
            AuctionDTO auctionDTO = (AuctionDTO) msg.getData();
            ItemDTO itemDTO = auctionDTO.getItem();
            User owner = UserDAO.getUserByUsername(itemDTO.getOwner().getUsername());
            Item item = ItemFactory.of(itemDTO.getType()).create(itemDTO.getId(),itemDTO.getName(),itemDTO.getDescription(),owner);
            Auction room = new Auction(
                    item,
                    auctionDTO.getStartingPrice(),
                    auctionDTO.getBuyOutPrice(),
                    auctionDTO.getTickSize(),
                    auctionDTO.getStartTime(),
                    auctionDTO.getEndTime());
            
            // Lưu item và auction vào database
            ItemDAO itemDAO = new ItemDAO();
            AuctionDAO auctionDAO = new AuctionDAO();
            int itemInserted = itemDAO.insert(item);
            int auctionInserted = auctionDAO.insert(room);
            
            if (itemInserted > 0 && auctionInserted > 0) {
                AuctionManager.getInstance().addRoom(room);
                AuctionManager.getInstance().broadCast(new NetworkMessage(
                        "UPDATE_BID",
                        (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms()))
                );
                sendMessage(new NetworkMessage("SELL_SUCCESS", true));
                log.info("SELL SUCCESS");
                activities.add(new Notification("you have sold item successfully", LocalTime.now()));
                log.info("Added SELL notification, total activities: {}", activities.size());
            } else {
                sendMessage(new NetworkMessage("SELL_FAILED", false));
                log.info("SELL FAIL");
                activities.add(new Notification("sell item failed", LocalTime.now()));
                log.info("Added SELL FAILED notification, total activities: {}", activities.size());
            }
        } else if ("LOGIN".equals(action)) {
            SignUpDTO dto = (SignUpDTO) msg.getData();
            User user = userService.login(dto.getUsername(), dto.getPassword());
            boolean isSuccess = user != null;
            sendMessage(new NetworkMessage("LOGIN", Mappers.toDTO(user)));
            log.info("{}{}", dto.getUsername(), isSuccess ? " successfully login" : " failed to login");
            activities.add(new Notification(isSuccess ? "login successfully" : "login failed", LocalTime.now()));
        } else if ("GET_PRODUCTS".equals(action)) {
            sendMessage(new NetworkMessage("GET_PRODUCTS",
                    (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())));

        } else if ("BUY_OUT".equals(action)) {
            BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();

            User bidder = UserDAO.getUserByUsername(transactionDTO.getBidder().getUsername());
            BidTransaction transaction = new BidTransaction(
                    AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId()),
                    bidder,
                    transactionDTO.getBidAmount());
            Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
            if (auction.buyOut(transaction)) {
                AuctionManager.getInstance().removeRoom(auction); // remove auction
                sendMessage(new NetworkMessage("BUYOUT_SUCCESS", null));
                AuctionManager.getInstance().broadCast(new NetworkMessage(
                        "UPDATE_BID",
                        (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms()))
                );
                log.info("BUY OUT SUCCESS");
                activities.add(new Notification("you have buy out item successfully", LocalTime.now()));
            }
        } else if ("GET_MY_LIST".equals(action)) {
            List<Auction> myList = new ArrayList<>();
            String userName = (String) msg.getData();
            for (Auction auction : AuctionManager.getInstance().getAllRooms()) {
                if (userName.equals(auction.getItem().getOwner().getUsername())) {
                    myList.add(auction);
                }
            }
            sendMessage(new NetworkMessage("GET_MY_LIST", (Serializable) Mappers.toAuctionDTOList(myList)));
        } else if ("CREATE_ACCOUNT".equals(action)) {
            SignUpDTO dto = (SignUpDTO) msg.getData();
            User newUser = new User(dto.getId(), dto.getUsername(), dto.getPassword());
            boolean isSuccess = userService.register(newUser);
            sendMessage(new NetworkMessage("CREATE_ACCOUNT", isSuccess));
            activities.add(new Notification(isSuccess ? "account created successfully" : "account creation failed", LocalTime.now()));
            log.info("{}{}", dto.getUsername(), isSuccess ? " successfully created account" : " failed to create account");
        } else if (action.equals("GET_ACTIVITIES")) {
            log.info("GET_ACTIVITIES request received, sending {} notifications", activities.size());
            sendMessage(new NetworkMessage("GET_ACTIVITIES", (Serializable) new ArrayList<>(activities)));
        }
    }

}
