package auction_server.service;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import auction_server.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auction_server.core.AuctionManager;
import auction_server.dao.DAOProvider;
import auction_server.entities.Auction;
import auction_server.entities.BidTransaction;
import auction_server.entities.Item;
import auction_server.entities.User;
import auction_server.factory.ItemFactory;
import auction_server.mapper.Mappers;
import auction_shared.Network.NetworkMessage;
import auction_shared.Network.Notification;
import auction_shared.dto.AuctionDTO;
import auction_shared.dto.AuctionStatus;
import auction_shared.dto.BidTransactionDTO;
import auction_shared.dto.EndedProductsRequest;
import auction_shared.dto.ItemDTO;
import auction_shared.dto.ProductListResponse;
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
    private static final int PAGE_SIZE_ENDED = 12;

    private final UserService userService;
    private final SellService sellService;
    private final DAOProvider daoProvider;
    private final List<Notification> activities = new CopyOnWriteArrayList<>();
    private User loggedInUser;
    private final MessageSender messageSender;
    
    /**
     * Interface để gửi message về client.
     */
    public interface MessageSender {
        void sendMessage(NetworkMessage msg);
    }

    /**
     * Khởi tạo MessageHandlerService.
     *
     * @param activities Danh sách hoạt động của user
     * @param messageSender Callback để gửi message về client
     *
     */
    public MessageHandlerService(MessageSender messageSender,
                                 DAOProvider daoProvider) {
        this.daoProvider = daoProvider;
        this.userService = new UserService(daoProvider);
        this.sellService = new SellService(daoProvider);
        this.messageSender = messageSender;
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

        User bidder = daoProvider.userDAO().getUserByUsername(transactionDTO.getBidder().getUsername());
        if (bidder == null) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
            activities.add(new Notification("bid failed: user not found", LocalTime.now()));
            return;
        }

        BidTransaction transaction = new BidTransaction(auction, bidder, transactionDTO.getBidAmount());
        BidService bidService = new BidService(daoProvider);
        try {
            bidService.processAndSaveBid(auction, transaction);
            messageSender.sendMessage(new NetworkMessage("BID_SUCCESS", Mappers.toDTO(auction)));
            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)
            ));
            log.info("A new bid has been placed");
            activities.add(new Notification("you have placed bid successfully", LocalTime.now()));
        }
        catch (InvalidBidAmountException | SelfBiddingException | InactiveBidException e) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.info(e.getMessage());
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
        }
        catch (DatabaseException e) {
            // Catch database errors
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.error(e.getMessage());
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
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
        User owner = daoProvider.userDAO().getUserByUsername(itemDTO.getOwner().getUsername());
        Item item = ItemFactory.of(itemDTO.getType()).create(itemDTO.getId(), itemDTO.getName(),
                itemDTO.getDescription(), owner, (String) itemDTO.getTypeSpecificAttribute());
        Auction room = new Auction(
                item,
                auctionDTO.getStartingPrice(),
                auctionDTO.getBuyOutPrice(),
                auctionDTO.getTickSize(),
                auctionDTO.getStartTime(),
                auctionDTO.getEndTime(),
                auctionDTO.isAntiSniping()
        );

        try {
            sellService.publishItemAndAuction(item, room);

            AuctionManager.getInstance().addRoom(room);
            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)
            ));
            messageSender.sendMessage(new NetworkMessage("SELL_SUCCESS", true));
            log.info("SELL SUCCESS");
            activities.add(new Notification("you have sold item successfully", LocalTime.now()));
            log.info("Added SELL notification, total activities: {}", activities.size());
        } catch (IllegalArgumentException e) {
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
        try {
            User user = userService.login(dto.getUsername(), dto.getPassword());
            boolean isSuccess = user != null;
            if (isSuccess) {
                setLoggedInUser(user);
            }
            messageSender.sendMessage(new NetworkMessage("LOGIN", Mappers.toDTO(user)));
            log.info("{}{}", dto.getUsername(), " successfully login");
            activities.add(new Notification("login successfully", LocalTime.now()));
        } catch (IllegalArgumentException | UserBannedException | UserNotFoundException | DatabaseException e) {
            messageSender.sendMessage(new NetworkMessage("LOGIN", null));
            log.info("{}{}", dto.getUsername(), " failed to login");
            activities.add(new Notification(
                    e.getMessage(),
                    LocalTime.now()
            ));
        }

    }
    
    /**
     * Xử lý lấy danh sách sản phẩm (hybrid).
     * - ACTIVE: lấy từ RAM (AuctionManager)
     * - ENDED/SOLD: lấy từ DB (phân trang, trang đầu)
     *
     * @param msg NetworkMessage
     */
    public void handleGetProducts(NetworkMessage msg) {
        List<Auction> activeFromRam = AuctionManager.getInstance().getAllRooms().stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());

        List<Auction> endedFromDb;
        User user = getLoggedInUser();
        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            endedFromDb = daoProvider.auctionDAO().selectAllAuctions().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ENDED || a.getStatus() == AuctionStatus.SOLD)
                    .collect(Collectors.toList());
        } else {
            endedFromDb = daoProvider.auctionDAO().selectEndedSaledAuctions(null, 0, PAGE_SIZE_ENDED);
        }
        int endedCount = daoProvider.auctionDAO().countEndedSaledAuctions(null);

        List<AuctionDTO> activeDTOs = Mappers.toAuctionDTOList(activeFromRam);
        List<AuctionDTO> endedDTOs = Mappers.toAuctionDTOList(endedFromDb);

        ProductListResponse response = new ProductListResponse(activeDTOs, endedDTOs, endedCount, activeFromRam.size());
        messageSender.sendMessage(new NetworkMessage("GET_PRODUCTS", response));
    }

    /**
     * Xử lý lấy thêm ENDED/SOLD auctions có phân trang từ DB.
     * Dùng khi client phân trang Ended tab.
     *
     * @param msg NetworkMessage chứa EndedProductsRequest
     */
    public void handleGetEndedProducts(NetworkMessage msg) {
        EndedProductsRequest req = (EndedProductsRequest) msg.getData();
        String category = req.getCategoryFilter();
        int page = req.getPage();
        int pageSize = req.getPageSize();

        List<Auction> endedFromDb = daoProvider.auctionDAO().selectEndedSaledAuctions(category, page, pageSize);
        int totalCount = daoProvider.auctionDAO().countEndedSaledAuctions(category);

        ProductListResponse response = new ProductListResponse(
                java.util.Collections.emptyList(),
                Mappers.toAuctionDTOList(endedFromDb),
                totalCount,
                0
        );
        messageSender.sendMessage(new NetworkMessage("GET_ENDED_PRODUCTS", response));
    }
    
    /**
     * Xử lý mua ngay.
     *
     * @param msg NetworkMessage chứa BidTransactionDTO
     */
    public void handleBuyOut(NetworkMessage msg) {
        BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();
        Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
        if (auction == null) {
            messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", null));
            return;
        }

        User bidder = daoProvider.userDAO().getUserByUsername(transactionDTO.getBidder().getUsername());
        if (bidder == null) {
            messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", "User not found"));
            return;
        }
        BidTransaction transaction = new BidTransaction(auction, bidder, transactionDTO.getBidAmount());

        BidService bidService = new BidService(daoProvider);

        try {
            bidService.processBuyOut(auction, transaction);
            AuctionDTO auctionDTO = Mappers.toDTO(auction);
            String winnerId = auction.getWinnerId();
            if (winnerId != null) {
                messageSender.sendMessage(new NetworkMessage("BUYOUT_SUCCESS", auctionDTO));
            }

            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)
            ));

            log.info("BUY OUT SUCCESS");
            activities.add(new Notification("you have buy out item successfully", LocalTime.now()));
        }
        catch (InvalidBidAmountException | InactiveBidException | SelfBiddingException e) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.info(e.getMessage());
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
        }
        catch (DatabaseException e) {
            messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", e.getMessage()));
            log.error(e.getMessage());
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
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
        try {
            userService.register(newUser);
            messageSender.sendMessage(new NetworkMessage("CREATE_ACCOUNT", true));
            activities.add(new Notification(
                    "account created successfully",
                    LocalTime.now()
            ));
            log.info("{}{}", dto.getUsername(),  " successfully created account");
        } catch (IllegalArgumentException | DatabaseException e) {
            messageSender.sendMessage(new NetworkMessage("CREATE_ACCOUNT", false));
            log.info("{}{}", dto.getUsername(), " failed to create account");
            activities.add(new Notification(
                    e.getMessage(),
                    LocalTime.now()
            ));
        }

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
        try {
            daoProvider.userDAO().userBan(userDTO);
            List<User> users = daoProvider.userDAO().getAllUsers();
            messageSender.sendMessage(new NetworkMessage("GET_USERS", (Serializable) Mappers.toUserDTOList(users)));
            AuctionManager.getInstance().broadcast(new NetworkMessage("BAN_USER", userDTO));
            activities.add(new Notification("ban user successfully", LocalTime.now()));

        } catch (DatabaseException e) {
            messageSender.sendMessage(new NetworkMessage("BAN_FAIL", null));
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
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
            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)
            ));
        }
    }
    
    /**
     * Xử lý lấy danh sách users.
     * 
     * @param msg NetworkMessage
     */
    public void handleGetUsers(NetworkMessage msg) {
        List<User> users = daoProvider.userDAO().getAllUsers();
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

        Auction auction = AuctionManager.getInstance().getRoom(itemId);
        if (auction != null) {
            history = Mappers.toBidTransactionDTOList(auction.getBidHistory());
        } else {
            history = Mappers.toBidTransactionDTOList(
                    daoProvider.bidTransactionDAO().selectByAuctionId(itemId)
            );
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

    }
}
