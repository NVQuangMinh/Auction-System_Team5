package auctionserver.service;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

//import auctionserver.exception.*;
import auctionserver.exception.DatabaseException;
import auctionserver.exception.InactiveBidException;
import auctionserver.exception.InvalidBidAmountException;
import auctionserver.exception.SelfBiddingException;
import auctionserver.exception.UserBannedException;
import auctionserver.exception.UserNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import auctionserver.core.AuctionManager;
import auctionserver.dao.DAOProvider;
import auctionserver.entities.Auction;
import auctionserver.entities.BidTransaction;
import auctionserver.entities.Item;
import auctionserver.entities.User;
import auctionserver.factory.ItemFactory;
import auctionserver.mapper.Mappers;
import auctionshared.Network.NetworkMessage;
import auctionshared.Network.Notification;
import auctionshared.dto.AuctionDTO;
import auctionshared.dto.AuctionStatus;
import auctionshared.dto.BidTransactionDTO;

import auctionshared.dto.ItemDTO;

import auctionshared.dto.SignUpDTO;
import auctionshared.dto.UserDTO;

/**
 * Service xử lý các message từ client
 */
public class MessageHandlerService {

    private static final Logger log = LoggerFactory.getLogger(MessageHandlerService.class);
    private static final int LIMIT_ENDED_PRODUCTS = 15;

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
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", "Cuộc đấu giá đã kết thúc hoặc không còn tồn tại nữa!"));
            activities.add(new Notification("Trả giá thất bại: không tìm thấy phiên đấu giá.", LocalTime.now()));
            return;
        }

        User bidder = daoProvider.userDAO().getUserByUsername(transactionDTO.getBidder().getUsername());
        if (bidder == null) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
            activities.add(new Notification("Trả giá thất bại: không tìm thấy người dùng.", LocalTime.now()));
            return;
        }

        BidTransaction transaction = new BidTransaction(auction, bidder, transactionDTO.getBidAmount());
        BidService bidService = new BidService(daoProvider);
        try {
            bidService.processAndSaveBid(auction, transaction);
            messageSender.sendMessage(new NetworkMessage("BID_SUCCESS", Mappers.toDTO(auction)));
            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    /**
                     * Cái dấu -> là lambda
                     * Ở bên trái: a (là phần tử được duyệt)
                     * Bên phải: điều kiện (if)
                     * Nếu đúng điều kiện (active) thì sẽ trả veef true cho hàm filter
                     */
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)));
            log.info("Một lượt trả giá mới vừa được ghi nhận.");
            activities.add(new Notification("Bạn đã trả giá sản phẩm thành công.", LocalTime.now()));
        } catch (InvalidBidAmountException | SelfBiddingException | InactiveBidException e) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.info(e.getMessage());
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
        } catch (DatabaseException e) {
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
                auctionDTO.isAntiSniping());

        try {
            sellService.publishItemAndAuction(item, room);

            AuctionManager.getInstance().addRoom(room);
            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    /**
                     * Cái dấu -> là lambda
                     * Ở bên trái: a (là phần tử được duyệt)
                     * Bên phải: điều kiện (if)
                     * Nếu đúng điều kiện (active) thì sẽ trả veef true cho hàm filter
                     */
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)));
            messageSender.sendMessage(new NetworkMessage("SELL_SUCCESS", true));
            log.info("SELL SUCCESS");
            activities.add(new Notification("Bạn đã đăng bán sản phẩm thành công.", LocalTime.now()));
            log.info("Đã thêm thông báo SELL, số hoạt động hiện tại: {}", activities.size());
        } catch (IllegalArgumentException e) {
            messageSender.sendMessage(new NetworkMessage("SELL_FAILED", false));
            log.info("SELL FAIL");
            activities.add(new Notification("Bạn đã đăng bán sản phẩm thất bại", LocalTime.now()));
            log.info("Đã thêm thông báo SELL FAILED, số hoạt động hiện tại: {}", activities.size());
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
                    LocalTime.now()));
        }

    }

    /**
     * Xử lý lấy danh sách sản phẩm Active.
     * ACTIVE: lấy từ RAM (AuctionManager)
     *
     * @param msg NetworkMessage
     */
    public void handleGetActiveProducts(NetworkMessage msg) {
        List<Auction> activeFromRam = AuctionManager.getInstance().getAllRooms().stream()
                .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());

        List<AuctionDTO> activeDTOs = Mappers.toAuctionDTOList(activeFromRam);

        messageSender.sendMessage(new NetworkMessage("GET_ACTIVE_PRODUCTS", (Serializable) activeDTOs));
    }

    /**
     * Xử lý lấy thêm ENDED/SOLD auctions có phân trang từ DB.
     * Dùng khi client phân trang Ended tab.
     *
     */
    public void handleGetEndedProducts(NetworkMessage msg) {
        String category = (String) msg.getData();

        List<Auction> endedFromDb = daoProvider.auctionDAO().selectEndedSaledAuctions(category, LIMIT_ENDED_PRODUCTS);

        messageSender.sendMessage(
                new NetworkMessage("GET_ENDED_PRODUCTS", (Serializable) Mappers.toAuctionDTOList(endedFromDb)));
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
            messageSender
                    .sendMessage(new NetworkMessage("BUYOUT_FAILED", "Phiên đấu giá đã kết thúc hoặc không còn tồn tại!"));
            return;
        }

        User bidder = daoProvider.userDAO().getUserByUsername(transactionDTO.getBidder().getUsername());
        if (bidder == null) {
            messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", "Không tìm thấy người dùng"));
            return;
        }
        BidTransaction transaction = new BidTransaction(auction, bidder, transactionDTO.getBidAmount());

        BidService bidService = new BidService(daoProvider);

        try {
            bidService.processBuyOut(auction, transaction);
            AuctionManager.getInstance().removeRoom(auction);
            AuctionDTO auctionDTO = Mappers.toDTO(auction);
            String winnerId = auction.getWinnerId();
            if (winnerId != null) {
                messageSender.sendMessage(new NetworkMessage("BUYOUT_SUCCESS", auctionDTO));
            }

            // Broadcast cho tất cả client biết auction đã SOLD (push-based, không cần
            // client query DB)
            AuctionManager.getInstance().broadcast(new NetworkMessage("AUCTION_SOLD", auctionDTO));
            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)));

            log.info("BUY OUT SUCCESS");
            activities.add(new Notification("Bạn đã mua sản phẩm thành công", LocalTime.now()));
        } catch (InvalidBidAmountException | InactiveBidException | SelfBiddingException e) {
            messageSender.sendMessage(new NetworkMessage("BID_FAILED", e.getMessage()));
            log.info(e.getMessage());
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
        } catch (DatabaseException e) {
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
                    LocalTime.now()));
            log.info("{}{}", dto.getUsername(), " đã tạo tài khoản mới thành công");
        } catch (IllegalArgumentException | DatabaseException e) {
            messageSender.sendMessage(new NetworkMessage("CREATE_ACCOUNT", false));
            log.info("{}{}", dto.getUsername(), " đã tạo tài khoản mới thất bại");
            activities.add(new Notification(
                    e.getMessage(),
                    LocalTime.now()));
        }

    }

    /**
     * Xử lý lấy danh sách hoạt động.
     * 
     * @param msg NetworkMessage
     */
    public void handleGetActivities(NetworkMessage msg) {
        log.info("Đã nhận yêu cầu GET_ACTIVITIES, đang gửi {} thông báo", activities.size());
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
            activities.add(new Notification("Khoá tài khoản người dùng thành công", LocalTime.now()));

        } catch (DatabaseException e) {
            messageSender.sendMessage(new NetworkMessage("BAN_FAIL", null));
            activities.add(new Notification(e.getMessage(), LocalTime.now()));
        }

    }

    /**
     * Xử lý xóa (ban) sản phẩm bởi Admin.
     * - Nếu ACTIVE (còn trong RAM): remove khỏi AuctionManager + đánh dấu BANNED
     * trong DB + broadcast UPDATE_BID.
     * - Nếu ENDED/SOLD (chỉ trong DB): đánh dấu BANNED trong DB + broadcast
     * REMOVE_ITEM để client xóa khỏi UI.
     *
     * @param msg NetworkMessage chứa AuctionDTO
     */
    public void handleRemoveItem(NetworkMessage msg) {
        AuctionDTO auctionDTO = (AuctionDTO) msg.getData();
        String itemId = auctionDTO.getItem().getId();

        Auction auction = AuctionManager.getInstance().getRoom(itemId);
        if (auction != null) {
            // ACTIVE: remove khỏi RAM trước, rồi ban trong DB
            AuctionManager.getInstance().removeRoom(auction);
            try {
                daoProvider.auctionDAO().updateStatusOnly(itemId, AuctionStatus.BANNED);
                log.info("Phiên đấu giá {} đã bị khoá (trạng thái ACTIVE)", itemId);
            } catch (DatabaseException e) {
                log.error("Không thể khoá phiên đấu giá {} trong cơ sở dữ liệu", itemId, e);
            }
            AuctionManager.getInstance().broadcast(new NetworkMessage("REMOVE_ITEM", auctionDTO));
            var activeOnly = AuctionManager.getInstance().getAllRooms().stream()
                    .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
                    .collect(Collectors.toList());
            AuctionManager.getInstance().broadcast(new NetworkMessage(
                    "UPDATE_BID",
                    (Serializable) Mappers.toAuctionDTOList(activeOnly)));
        } else {
            // ENDED/SOLD: chỉ tồn tại trong DB
            try {
                daoProvider.auctionDAO().updateStatusOnly(itemId, AuctionStatus.BANNED);
                log.info("Phiên đấu giá {} đã bị khoá (trạng thái ENDED/SOLD)", itemId);
                // Thông báo cho tất cả client Admin xóa item khỏi danh sách
                AuctionManager.getInstance().broadcast(new NetworkMessage(
                        "REMOVE_ITEM", auctionDTO));
            } catch (DatabaseException e) {
                log.error("Không thể khoá phiên đấu giá {} trong cơ sở dữ liệu", itemId, e);
            }
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
                    daoProvider.bidTransactionDAO().selectByAuctionId(itemId));
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

    /**
     * Số lượng hoạt động - thông báo.
     *
     * @return số lượng activites
     */
    public int getActivitiesSize() {
        return activities.size();
    }
}
