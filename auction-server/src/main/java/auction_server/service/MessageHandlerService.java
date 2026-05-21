package auction_server.service;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auction_server.Network.ClientHandler;
import auction_server.core.AuctionManager;
import auction_server.dao.AuctionDAO;
import auction_server.dao.BidTransactionDAO;
import auction_server.dao.ItemDAO;
import auction_server.dao.UserDAO;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.factory.ItemFactory;
import auction_server.mapper.Mappers;
import auction_shared.Network.NetworkMessage;
import auction_shared.Network.Notification;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.BidTransactionDTO;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.SignUpDTO;
import auction_shared.dto.UserDTO;

/**
 * Service xử lý các message từ client.
 * 
 * Class này chứa logic xử lý cho tất cả các loại action từ client,
 * bao gồm đặt giá thầu, bán sản phẩm, đăng nhập, đăng ký, v.v.
 * 
 * @author Team 5
 * @version 1.0
 */
public class MessageHandlerService {
    
    private static final Logger log = LoggerFactory.getLogger(MessageHandlerService.class);

    private final UserService userService;
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final ItemDAO itemDAO;
    private final SellService sellService;
    private final List<Notification> activities;
    private User loggedInUser;
    private final MessageSender messageSender;
    private final LogoutHandler logoutHandler;
    
    /**
     * Interface để gửi message về client.
     */
    public interface MessageSender {
        void sendMessage(NetworkMessage msg);
    }

    /**
     * Interface để xử lý logout — cần remove client khỏi AuctionManager.
     */
    public interface LogoutHandler {
        void onLogout();
    }

    /**
     * Khởi tạo MessageHandlerService.
     *
     * @param activities Danh sách hoạt động của user
     * @param messageSender Callback để gửi message về client
     * @param logoutHandler Callback khi user logout
     */
    public MessageHandlerService(List<Notification> activities, MessageSender messageSender, LogoutHandler logoutHandler) {
        this.userService = new UserService();
        this.auctionDAO = new AuctionDAO();
        this.bidTransactionDAO = new BidTransactionDAO();
        this.itemDAO = new ItemDAO();
        this.sellService = new SellService();
        this.activities = activities;
        this.messageSender = messageSender;
        this.logoutHandler = logoutHandler;
    }
    
    /**
     * Thiết lập user đã đăng nhập.
     * 
     * @param user User đã đăng nhập
     */
    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }
    
    /**
     * Lấy user đã đăng nhập.
     * 
     * @return User đã đăng nhập
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }
    
    /**
     * Xử lý đặt giá bid.
     * 
     * @param msg NetworkMessage chứa BidTransactionDTO
     */
    public void handlePlaceBid(NetworkMessage msg) {
        BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();

        Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
        if (auction == null) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
            activities.add(new Notification("bid failed: auction not found", LocalTime.now()));
            return;
        }

        User bidder = UserDAO.getUserByUsername(transactionDTO.getBidder().getUsername());
        if (bidder == null) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
            activities.add(new Notification("bid failed: user not found", LocalTime.now()));
            return;
        }

        BidTransaction transaction = new BidTransaction(auction, bidder, transactionDTO.getBidAmount());
        BidService bidService = new BidService();
        boolean isSuccess = bidService.processAndSaveBid(auction, transaction);

        if (isSuccess) {
            messageSender.sendMessage(new NetworkMessage("BID_SUCCESS", Mappers.toDTO(auction)));
            AuctionManager.getInstance().broadCast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())
            ));
            log.info("A new bid has been placed");
            activities.add(new Notification("you have placed bid successfully", LocalTime.now()));
        } else {
            log.info("Your bid has failed");
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
            activities.add(new Notification("Your bid has failed", LocalTime.now()));
        }
    }
    
    /**
     * Xử lý bán sản phẩm.
     * 
     * @param msg NetworkMessage chứa AuctionDTO
     */
    public void handleSell(NetworkMessage msg) {
        AuctionDTO auctionDTO = (AuctionDTO) msg.getData();
        ItemDTO itemDTO = auctionDTO.getItem();
        User owner = UserDAO.getUserByUsername(itemDTO.getOwner().getUsername());
        Item item = ItemFactory.of(itemDTO.getType()).create(itemDTO.getId(), itemDTO.getName(),
                itemDTO.getDescription(), owner);
        Auction room = new Auction(
                item,
                auctionDTO.getStartingPrice(),
                auctionDTO.getBuyOutPrice(),
                auctionDTO.getTickSize(),
                auctionDTO.getStartTime(),
                auctionDTO.getEndTime(),
                auctionDTO.isAntiSniping()
        );

        boolean isSuccess = sellService.publishItemAndAuction(item, room);

        if (isSuccess) {
            AuctionManager.getInstance().addRoom(room);
            AuctionManager.getInstance().broadCast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms()))
            );
            messageSender.sendMessage(new NetworkMessage("SELL_SUCCESS", true));
            log.info("SELL SUCCESS");
            activities.add(new Notification("you have sold item successfully", LocalTime.now()));
            log.info("Added SELL notification, total activities: {}", activities.size());
        } else {
            messageSender.sendMessage(new NetworkMessage("SELL_FAILED", false));
            log.info("SELL FAIL");
            activities.add(new Notification("sell item failed", LocalTime.now()));
            log.info("Added SELL FAILED notification, total activities: {}", activities.size());
        }
    }
    
    /**
     * Xử lý đăng nhập.
     * 
     * @param msg NetworkMessage chứa SignUpDTO
     */
    public void handleLogin(NetworkMessage msg) {
        SignUpDTO dto = (SignUpDTO) msg.getData();
        User user = userService.login(dto.getUsername(), dto.getPassword());
        boolean isSuccess = user != null;
        if (isSuccess) {
            this.loggedInUser = user;
        }
        messageSender.sendMessage(new NetworkMessage("LOGIN", Mappers.toDTO(user)));
        log.info("{}{}", dto.getUsername(), isSuccess ? " successfully login" : " failed to login");
        activities.add(new Notification(isSuccess ? "login successfully" : "login failed", LocalTime.now()));
    }
    
    /**
     * Xử lý lấy danh sách sản phẩm.
     * 
     * @param msg NetworkMessage
     */
    public void handleGetProducts(NetworkMessage msg) {
        messageSender.sendMessage(new NetworkMessage("GET_PRODUCTS",
                (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())));
    }
    
    /**
     * Xử lý mua ngay.
     *
     * @param msg NetworkMessage chứa BidTransactionDTO
     */
    public void handleBuyOut(NetworkMessage msg) {
        BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();
        User bidder = UserDAO.getUserByUsername(transactionDTO.getBidder().getUsername());

        BidTransaction transaction = new BidTransaction(
                AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId()),
                bidder,
                transactionDTO.getBidAmount());
        Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());

        if (auction == null) {
            messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", null));
            return;
        }

        BidService bidService = new BidService();
        boolean isSuccess = bidService.processBuyOut(auction, transaction);

        if (isSuccess) {
            AuctionManager.getInstance().removeRoom(auction);
            AuctionDTO auctionDTO = Mappers.toDTO(auction);

            String winnerId = auction.getWinnerId();
            if (winnerId != null) {
                for (ClientHandler client : AuctionManager.getInstance().getActiveClients()) {
                    User u = client.getLoggedInUser();
                    if (u != null && winnerId.equals(u.getId())) {
                        client.sendMessage(new NetworkMessage("YOU_WON", auctionDTO));
                        break;
                    }
                }
            }

            AuctionManager.getInstance().broadCast(new NetworkMessage("AUCTION_ENDED", auctionDTO));
            AuctionManager.getInstance().broadCast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms()))
            );

            messageSender.sendMessage(new NetworkMessage("BUYOUT_SUCCESS", null));
            log.info("BUY OUT SUCCESS");
            activities.add(new Notification("you have buy out item successfully", LocalTime.now()));
        } else {
            messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", null));
        }
    }
    
    /**
     * Xử lý lấy danh sách sản phẩm của user.
     * 
     * @param msg NetworkMessage chứa username
     */
    public void handleGetMyList(NetworkMessage msg) {
        List<Auction> myList = new ArrayList<>();
        String userName = (String) msg.getData();
        for (Auction auction : AuctionManager.getInstance().getAllRooms()) {
            if (userName.equals(auction.getItem().getOwner().getUsername())) {
                myList.add(auction);
            }
        }
        messageSender.sendMessage(new NetworkMessage("GET_MY_LIST", (Serializable) Mappers.toAuctionDTOList(myList)));
    }
    
    /**
     * Xử lý tạo tài khoản.
     * 
     * @param msg NetworkMessage chứa SignUpDTO
     */
    public void handleCreateAccount(NetworkMessage msg) {
        SignUpDTO dto = (SignUpDTO) msg.getData();
        User newUser = new User(dto.getId(), dto.getUsername(), dto.getPassword());
        boolean isSuccess = userService.register(newUser);
        messageSender.sendMessage(new NetworkMessage("CREATE_ACCOUNT", isSuccess));
        activities.add(new Notification(isSuccess ? "account created successfully" : "account creation failed", 
                LocalTime.now()));
        log.info("{}{}", dto.getUsername(), isSuccess ? " successfully created account" : " failed to create account");
    }
    
    /**
     * Xử lý lấy danh sách hoạt động.
     * 
     * @param msg NetworkMessage
     */
    public void handleGetActivities(NetworkMessage msg) {
        log.info("GET_ACTIVITIES request received, sending {} notifications", activities.size());
        messageSender.sendMessage(new NetworkMessage("GET_ACTIVITIES", (Serializable) activities));
    }
    
    /**
     * Xử lý ban user.
     * 
     * @param msg NetworkMessage chứa UserDTO
     */
    public void handleBanUser(NetworkMessage msg) {
        UserDTO userDTO = (UserDTO) msg.getData();
        if (UserDAO.userBan(userDTO)) {
            List<User> users = UserDAO.getAllUsers();
            messageSender.sendMessage(new NetworkMessage("GET_USERS", (Serializable) Mappers.toUserDTOList(users)));
            AuctionManager.getInstance().broadCast(new NetworkMessage("BAN_USER", userDTO));
        } else {
            messageSender.sendMessage(new NetworkMessage("BAN_FAIL", null));
        }
    }
    
    /**
     * Xử lý xóa sản phẩm.
     * 
     * @param msg NetworkMessage chứa AuctionDTO
     */
    public void handleRemoveItem(NetworkMessage msg) {
        AuctionDTO auctionDTO = (AuctionDTO) msg.getData();
        Auction auction = AuctionManager.getInstance().getRoom(auctionDTO.getItem().getId());
        if (auction != null) {
            AuctionManager.getInstance().removeRoom(auction);
            AuctionManager.getInstance().broadCast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms()))
            );
        }
    }
    
    /**
     * Xử lý lấy danh sách users.
     * 
     * @param msg NetworkMessage
     */
    public void handleGetUsers(NetworkMessage msg) {
        List<User> users = UserDAO.getAllUsers();
        messageSender.sendMessage(new NetworkMessage("GET_USERS", (Serializable) Mappers.toUserDTOList(users)));
    }
    
    /**
     * Xử lý lấy lịch sử đặt giá.
     * Hybrid: ACTIVE auction → đọc từ RAM
     * ENDED/SOLD auction → đọc từ DB.
     *
     * @param msg NetworkMessage chứa AuctionDTO
     */
    public void handleGetBidHistory(NetworkMessage msg) {
        AuctionDTO auctionDTO = (AuctionDTO) msg.getData();
        String itemId = auctionDTO.getItem().getId();

        List<BidTransactionDTO> history;

        // Thử đọc từ RAM trước (ACTIVE auction)
        Auction auction = AuctionManager.getInstance().getRoom(itemId);

        if (auction != null) {
            // ACTIVE → Đọc từ RAM (performance)
            history = Mappers.toBidTransactionDTOList(auction.getBidHistory());
        } else {
            // ENDED/SOLD → Đọc từ DB (durability)
            List<BidTransaction> dbHistory = bidTransactionDAO.selectByAuctionId(itemId);
            history = Mappers.toBidTransactionDTOList(dbHistory);
        }

        messageSender.sendMessage(new NetworkMessage("GET_BID_HISTORY", (Serializable) history));
    }
    
    /**
     * Xử lý đăng xuất.
     * 
     * @param msg NetworkMessage
     */
    public void handleLogout(NetworkMessage msg) {
        this.loggedInUser = null;
        activities.clear();
        if (logoutHandler != null) {
            logoutHandler.onLogout();
        }
    }
}
