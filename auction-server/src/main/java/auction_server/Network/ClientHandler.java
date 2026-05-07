package auction_server.Network;

import auction_server.core.AuctionManager;
import auction_server.entities.Auction;
import auction_server.entities.User;
import auction_server.mapper.Mappers;
import auction_server.service.AuctionService;
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

    // <<< THAY ĐỔI: Nhận Service và DAO thông qua constructor (Dependency Injection)
    // Điều này giúp việc testing dễ dàng hơn, nhưng hiện tại ta sẽ khởi tạo ở đây cho đơn giản.
    private final UserService userService;
    private final AuctionService auctionService;

    private User authenticatedUser;

    public ClientHandler(Socket socket, UserService userService, AuctionService auctionService) {
        this.socket = socket;
        this.userService = userService;
        this.auctionService = auctionService;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            AuctionManager.getInstance().addClient(this);

            // Vòng lặp chính để xử lý yêu cầu từ client
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

    // Gửi tin nhắn tới client này
    public void sendMessage(NetworkMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            log.error("Failed to send message to client", e);
        }
    }

    // <<< THAY ĐỔI: Phân luồng request một cách rõ ràng
    private void handleRequest(NetworkMessage msg) {
        String action = msg.getAction();

        // Các action không cần đăng nhập
        if ("LOGIN".equals(action)) {
            handleLogin(msg);
            return;
        } else if ("CREATE_ACCOUNT".equals(action)) {
            handleSignUp(msg);
            return;
        }

        // Yêu cầu xác thực cho các action còn lại
        if (authenticatedUser == null) {
            sendMessage(new NetworkMessage("ERROR", "Authentication required. Please log in."));
            return;
        }

        // Các action cần đăng nhập
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
            // TODO: Thêm case cho BUY_OUT, SELL_ITEM...
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
            userService.register(dto.getUsername(), dto.getPassword());
            sendMessage(new NetworkMessage("SIGNUP_SUCCESS", null));
        } catch (Exception e) {
            sendMessage(new NetworkMessage("SIGNUP_FAILED", e.getMessage()));
        }
    }

    // <<< THAY ĐỔI: Gọi service để lấy danh sách sản phẩm
    private void handleGetProducts() {
        try {
            List<Auction> auctions = auctionService.getActiveAuctions();
            List<AuctionDTO> dtos = Mappers.toAuctionDTOList(auctions);
            sendMessage(new NetworkMessage("PRODUCT_LIST", (Serializable) dtos));
        } catch (Exception e) {
            sendMessage(new NetworkMessage("ERROR", "Could not fetch products: " + e.getMessage()));
        }
    }

    // <<< THAY ĐỔI: Gọi service để lấy sản phẩm của cá nhân
    private void handleGetMyProducts() {
        try {
            List<Auction> auctions = auctionService.getAuctionsByOwner(authenticatedUser.getId());
            List<AuctionDTO> dtos = Mappers.toAuctionDTOList(auctions);
            sendMessage(new NetworkMessage("MY_PRODUCT_LIST", (Serializable) dtos));
        } catch (Exception e) {
            sendMessage(new NetworkMessage("ERROR", "Could not fetch your products: " + e.getMessage()));
        }
    }

    // <<< THAY ĐỔI: Logic đặt giá được đơn giản hóa và gọi service
    private void handlePlaceBid(NetworkMessage msg) {
        try {
            PlaceBidRequestDTO dto = (PlaceBidRequestDTO) msg.getData();

            // Gọi service
            Auction updatedAuction = auctionService.placeBid(dto.getAuctionId(), authenticatedUser, dto.getAmount());

            // gửi ặt thành công
            sendMessage(new NetworkMessage("BID_SUCCESS", "Your bid was placed successfully."));

            // Broadcast thông tin cập nhật
            AuctionUpdateDTO updateInfo = new AuctionUpdateDTO(
                    updatedAuction.getId(),
                    updatedAuction.getCurrentHighestBid(),
                    authenticatedUser.getUsername()
            );
            AuctionManager.getInstance().broadCast(new NetworkMessage("AUCTION_UPDATE", updateInfo));

        } catch (Exception e) {
            // lỗi đặt fail
            sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.error("Error placing bid for user '{}'", authenticatedUser.getUsername(), e);
        }
    }
}