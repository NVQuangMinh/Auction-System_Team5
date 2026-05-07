package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_server.entities.Auction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.mapper.Mappers;
import auction_server.service.AuctionService;
import auction_server.service.ItemService;
import auction_server.service.UserService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final UserService userService;
    private final AuctionService auctionService;
    private final ItemService itemService; // Mới

    private User authenticatedUser;

    public ClientHandler(Socket socket, UserService userService, AuctionService auctionService, ItemService itemService) {
        this.socket = socket;
        this.userService = userService;
        this.auctionService = auctionService;
        this.itemService = itemService; // Mới
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            AuctionManager.getInstance().addClient(this);

            while (true) {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                handleRequest(msg);
            }
        } catch (Exception e) {
            log.info("Client disconnected: {}", socket.getRemoteSocketAddress());
        } finally {
            AuctionManager.getInstance().removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }
        }
    }

    public void sendMessage(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            log.error("Failed to send message to client", e);
        }
    }

    private void handleRequest(NetworkMessage msg) {
        String action = msg.getAction();

        if ("LOGIN".equals(action)) {
            handleLogin(msg);
            return;
        } else if ("CREATE_ACCOUNT".equals(action)) {
            handleSignUp(msg);
            return;
        }

        if (authenticatedUser == null) {
            sendMessage(new NetworkMessage("ERROR", "Authentication required. Please log in."));
            return;
        }

        switch (action) {
            case "GET_PRODUCTS":
                handleGetProducts();
                break;
            case "GET_MY_LIST":
                handleGetMyProducts();
                break;
            case "PLACE_BID":
                handlePlaceBid(msg);
                break;
            case "CREATE_ITEM": // Mới
                handleCreateItem(msg);
                break;
            default:
                sendMessage(new NetworkMessage("ERROR", "Unknown action: " + action));
                break;
        }
    }

    private void handleLogin(NetworkMessage msg) {
        SignUpDTO dto = (SignUpDTO) msg.getData();
        try {
            User user = userService.login(dto.getUsername(), dto.getPassword());
            this.authenticatedUser = user;
            sendMessage(new NetworkMessage("LOGIN_SUCCESS", Mappers.toUserDTO(user)));
            log.info("User '{}' logged in successfully.", user.getUsername());
        } catch (Exception e) {
            sendMessage(new NetworkMessage("LOGIN_FAILED", e.getMessage()));
            log.warn("Failed login attempt for username '{}'.", dto.getUsername());
        }
    }

    private void handleSignUp(NetworkMessage msg) {
        SignUpDTO dto = (SignUpDTO) msg.getData();
        try {
            System.out.println("Da vao ClientHandler");
            userService.register(dto.getUsername(), dto.getPassword());
            sendMessage(new NetworkMessage("CREATE_ACCOUNT", (Boolean) true));
            log.info("User '{}' registered successfully.", dto.getUsername());
            System.out.println("ClientHandler da gui xong");
        } catch (Exception e) {
            sendMessage(new NetworkMessage("SIGNUP_FAILED", e.getMessage()));
            System.out.println(e.getMessage());
            log.info("User '{}' registered not successfully.", dto.getUsername());
        }
    }

    private void handleGetProducts() {
        try {
            List<Auction> auctions = auctionService.getActiveAuctions();
            List<AuctionDTO> dtos = Mappers.toAuctionDTOList(auctions);
            sendMessage(new NetworkMessage("PRODUCT_LIST", (Serializable) dtos));
        } catch (Exception e) {
            sendMessage(new NetworkMessage("ERROR", "Could not fetch products: " + e.getMessage()));
        }
    }
    
    private void handleGetMyProducts() {
        try {
            List<Auction> auctions = auctionService.getAuctionsByOwner(authenticatedUser.getId());
            List<AuctionDTO> dtos = Mappers.toAuctionDTOList(auctions);
            sendMessage(new NetworkMessage("MY_PRODUCT_LIST", (Serializable) dtos));
        } catch (Exception e) {
            sendMessage(new NetworkMessage("ERROR", "Could not fetch your products: " + e.getMessage()));
        }
    }

    private void handlePlaceBid(NetworkMessage msg) {
        try {
            PlaceBidRequestDTO dto = (PlaceBidRequestDTO) msg.getData();
            Auction updatedAuction = auctionService.placeBid(dto.getAuctionId(), authenticatedUser, dto.getAmount());
            sendMessage(new NetworkMessage("BID_SUCCESS", "Your bid was placed successfully."));

            AuctionUpdateDTO updateInfo = new AuctionUpdateDTO(
                updatedAuction.getId(),
                updatedAuction.getCurrentHighestBid(),
                authenticatedUser.getUsername()
            );
            AuctionManager.getInstance().broadCast(new NetworkMessage("AUCTION_UPDATE", updateInfo));

        } catch (Exception e) {
            sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.error("Error placing bid for user '{}'", authenticatedUser.getUsername(), e);
        }
    }

    private void handleCreateItem(NetworkMessage msg) {
        try {
            CreateItemRequestDTO dto = (CreateItemRequestDTO) msg.getData();
            Item newItem = itemService.createAndSaveItem(authenticatedUser, dto);
            // Gửi lại thông tin item đã được tạo (bao gồm ID mới) cho client
            sendMessage(new NetworkMessage("CREATE_ITEM_SUCCESS", Mappers.toItemDTO(newItem)));
            log.info("User '{}' created a new item '{}' with ID {}.", authenticatedUser.getUsername(), newItem.getName(), newItem.getId());
        } catch (Exception e) {
            sendMessage(new NetworkMessage("CREATE_ITEM_FAILED", e.getMessage()));
            log.error("Error creating item for user '{}'", authenticatedUser.getUsername(), e);
        }
    }
}
