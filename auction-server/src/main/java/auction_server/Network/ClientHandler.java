package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_server.entities.Auction;
import auction_server.entities.User;
import auction_server.service.AuctionService;
import auction_server.service.UserService;
import auction_shared.Network.NetworkMessage;
import auction_shared.dto.BidTransactionDTO;
import auction_shared.dto.SignUpDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final UserService userService = new UserService();
    private final AuctionService auctionService = new AuctionService();
    private User authenticatedUser; // Keep track of the logged-in user

    public ClientHandler(Socket socket) {
        this.socket = socket;
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
            log.info("Client disconnected: " + socket.getRemoteSocketAddress());
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

        // Actions that don't require login
        if ("LOGIN".equals(action)) {
            handleLogin(msg);
            return;
        } else if ("CREATE_ACCOUNT".equals(action)) {
            handleSignUp(msg);
            return;
        }

        // All other actions require a logged-in user
        if (authenticatedUser == null) {
            sendMessage(new NetworkMessage("ERROR", "Authentication required. Please log in."));
            return;
        }

        switch (action) {
            case "PLACE_BID":
                handlePlaceBid(msg);
                break;
            case "GET_PRODUCTS":
                handleGetProducts();
                break;
            // TODO: Add cases for other actions like "SELL", "BUY_OUT", "GET_MY_LIST"
            default:
                sendMessage(new NetworkMessage("ERROR", "Unknown action: " + action));
                break;
        }
    }

    private void handleLogin(NetworkMessage msg) {
        SignUpDTO dto = (SignUpDTO) msg.getData();
        User user = userService.login(dto.getUsername(), dto.getPassword());
        if (user != null) {
            this.authenticatedUser = user;
            // TODO: You need to fetch the User's roles/profiles from the database here
            // and set them on the user object.
            // e.g., user.setBidderProfile(new BidderProfile());
            sendMessage(new NetworkMessage("LOGIN_SUCCESS", null /* Send UserDTO if needed */));
            log.info("User '{}' logged in successfully.", user.getUsername());
        } else {
            sendMessage(new NetworkMessage("LOGIN_FAILED", "Invalid username or password."));
            log.warn("Failed login attempt for username '{}'.", dto.getUsername());
        }
    }

    private void handleSignUp(NetworkMessage msg) {
        SignUpDTO dto = (SignUpDTO) msg.getData();
        // TODO: The User constructor expects a password hash. You should hash the password here.
        // This is a major security concern to address.
        User newUser = new User(null, dto.getUsername(), dto.getPassword()); // ID should be null, DB will generate it
        boolean isSuccess = userService.register(newUser);
        sendMessage(new NetworkMessage(isSuccess ? "SIGNUP_SUCCESS" : "SIGNUP_FAILED", null));
    }

    private void handlePlaceBid(NetworkMessage msg) {
        try {
            BidTransactionDTO dto = (BidTransactionDTO) msg.getData();
            // TODO: The DTO needs to be updated to send only the auction ID, not the whole object.
            Long auctionId = dto.getAuction().getId();
            Auction auction = AuctionManager.getInstance().getAuction(auctionId);

            if (auction != null) {
                auctionService.placeBid(auction, authenticatedUser, dto.getBidAmount());
                sendMessage(new NetworkMessage("BID_SUCCESS", null));

                // Notify all clients about the update
                // TODO: Send a more specific DTO with the updated price and bidder info.
                AuctionManager.getInstance().broadCast(new NetworkMessage("AUCTION_UPDATE", null));
            } else {
                sendMessage(new NetworkMessage("ERROR", "Auction not found."));
            }
        } catch (Exception e) {
            sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.error("Error placing bid", e);
        }
    }

    private void handleGetProducts() {
        // TODO: The DTO mapping logic needs to be updated to handle the new entity structures.
        // List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();
        // List<AuctionDTO> dtos = Mappers.toAuctionDTOList(auctions);
        // sendMessage(new NetworkMessage("PRODUCT_LIST", (Serializable) dtos));
        sendMessage(new Network-Message("TODO", "GetProducts needs to be re-implemented with new DTOs."));
    }
}
