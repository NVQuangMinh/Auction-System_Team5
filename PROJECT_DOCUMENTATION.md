# Tài Liệu Hệ Thống Đấu Giá AuctionTeam5

## Mục Lục

1. [Tổng Quan Hệ Thống](#1-tổng-quan-hệ-thống)
2. [Mô Hình 3-Layer Chi Tiết](#2-mô-hình-3-layer-chi-tiết)
3. [Module Server (auction-server)](#3-module-server-auction-server)
4. [Module Client (auction-client)](#4-module-client-auction-client)
5. [Module Shared (auction-shared)](#5-module-shared-auction-shared)
6. [Các Design Patterns Sử Dụng](#6-các-design-patterns-sử-dụng)
7. [Chi Tiết Từng Method và Luồng Hoạt Động](#7-chi-tiết-từng-method-và-luồng-hoạt-động)
8. [Cơ Chế Anti-Sniping](#8-cơ-chế-anti-sniping)
9. [Cơ Chế Thread-Safety](#9-cơ-chế-thread-safety)
10. [Phục Hồi Database (Server Restart)](#10-phục-hồi-database-server-restart)

---

## 1. Tổng Quan Hệ Thống

### 1.1 Giới Thiệu

**AuctionTeam5** là một hệ thống đấu giá trực tuyến được xây dựng trên nền tảng **JavaFX** (phía client) kết hợp với **Java Socket** (giao tiếp mạng). Hệ thống cho phép người dùng đăng nhập, đăng ký tài khoản, đăng sản phẩm đấu giá, đặt giá thầu (bid), mua ngay (buy-out), và quản lý các phiên đấu giá theo thời gian thực.

### 1.2 Kiến Trúc Tổng Quan

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CLIENT (auction-client)                       │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                      JavaFX Application                         │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │    │
│  │  │   SignIn/Up  │  │    Scenes    │  │   Controllers        │  │    │
│  │  │   Controllers│  │  (FXML/GUI)  │  │   (Business Logic)   │  │    │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘  │    │
│  │                            │                                     │    │
│  │                   ┌────────▼────────┐                            │    │
│  │                   │  ClientService  │  (Singleton, Socket)      │    │
│  │                   │  (Observer)      │                            │    │
│  │                   └────────┬────────┘                            │    │
│  └────────────────────────────│───────────────────────────────────────┘    │
│                               │ TCP Socket (Object Streams)                │
└───────────────────────────────│─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          SERVER (auction-server)                        │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    SocketServer + ClientHandler                  │    │
│  │                            │                                      │    │
│  │  ┌─────────────────────────▼───────────────────────────┐        │    │
│  │  │              MessageHandlerService                    │        │    │
│  │  │   (Điều phối các action: LOGIN, BID, BUY_OUT...)     │        │    │
│  │  └─────────────────────────┬───────────────────────────┘        │    │
│  │                            │                                      │    │
│  │  ┌─────────────┐  ┌────────┴───────┐  ┌────────────────┐        │    │
│  │  │UserService  │  │  BidService    │  │  SellService    │        │    │
│  │  └──────┬──────┘  └───────┬────────┘  └───────┬────────┘        │    │
│  │         │                 │                  │                  │    │
│  │         └─────────────────┼──────────────────┘                  │    │
│  │                           ▼                                     │    │
│  │  ┌─────────────────────────────────────────────────────────┐    │    │
│  │  │                  DAO Layer                               │    │    │
│  │  │  (AuctionDAO, BidTransactionDAO, ItemDAO, UserDAO)     │    │    │
│  │  └────────────────────────┬────────────────────────────────┘    │    │
│  │                           │                                     │    │
│  │  ┌────────────────────────▼────────────────────────────────┐    │    │
│  │  │            DatabaseConnection (HikariCP)                  │    │    │
│  │  └────────────────────────┬────────────────────────────────┘    │    │
│  └──────────────────────────│───────────────────────────────────────┘    │
│                             ▼                                              │
│                    ┌─────────────────┐                                   │
│                    │   MySQL DB       │                                   │
│                    └─────────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                      SHARED (auction-shared)                            │
│  ┌──────────────────────┐  ┌──────────────────────┐                    │
│  │   NetworkMessage     │  │   Notification       │                    │
│  └──────────────────────┘  └──────────────────────┘                    │
│  ┌──────────────────────┐  ┌──────────────────────┐                    │
│  │      AuctionDTO      │  │      ItemDTO         │                    │
│  └──────────────────────┘  └──────────────────────┘                    │
│  ┌──────────────────────┐  ┌──────────────────────┐                    │
│  │   BidTransactionDTO   │  │      UserDTO         │                    │
│  └──────────────────────┘  └──────────────────────┘                    │
│  ┌──────────────────────┐  ┌──────────────────────┐                    │
│  │    AuctionStatus      │  │      ItemType         │                    │
│  └──────────────────────┘  └──────────────────────┘                    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Các Thành Phần Chính

| Thành phần | Mô tả | Công nghệ |
|------------|-------|-----------|
| **auction-client** | Ứng dụng JavaFX GUI cho người dùng | JavaFX, FXML |
| **auction-server** | Server xử lý logic đấu giá và socket | Java, SLF4J |
| **auction-shared** | DTOs và message objects dùng chung | Java Serializable |

### 1.4 Cổng Kết Nối

- **Server Socket**: Port `8080` - lắng nghe kết nối từ clients
- **Database**: MySQL - thông tin kết nối lấy từ environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`)

---

## 2. Mô Hình 3-Layer Chi Tiết

### 2.1 Presentation Layer (Client)

**Vị trí**: `auction-client/src/main/java/auction_client/`

**Trách nhiệm**:
- Hiển thị giao diện người dùng (UI)
- Xử lý tương tác người dùng (events)
- Gửi yêu cầu lên server qua `ClientService`
- Nhận và hiển thị phản hồi từ server

**Các thành phần chính**:

| Class | Mô tả |
|-------|-------|
| `Launcher` | Điểm khởi đầu, kết nối server và khởi chạy JavaFX |
| `ClientService` | Singleton quản lý kết nối Socket, gửi/nhận message |
| `SignInController` | Xử lý đăng nhập |
| `WebMenuBarController` | Điều hướng giữa các scene |
| `UserSession` | Lưu trữ thông tin user đang đăng nhập |

**Các Scene FXML**:

| Scene | Mô tả |
|-------|-------|
| `SignInScene.fxml` | Màn hình đăng nhập |
| `SignUpScene.fxml` | Màn hình đăng ký |
| `AuctionMain.fxml` | Màn hình chính sau khi đăng nhập |
| `BidProductScene.fxml` | Trang xem sản phẩm để bid |
| `SellProductScene.fxml` | Trang quản lý sản phẩm đã đăng |
| `ArtScene.fxml` | Lọc sản phẩm theo loại Arts |
| `ElectronicScene.fxml` | Lọc sản phẩm theo loại Electronics |
| `VehicleScene.fxml` | Lọc sản phẩm theo loại Vehicles |
| `ActivitiesScene.fxml` | Trang thông báo hoạt động |
| `AdminControlPanel.fxml` | Trang quản trị cho ADMIN |

### 2.2 Business Logic / Service Layer (Server)

**Vị trí**: `auction-server/src/main/java/auction_server/service/`

**Trách nhiệm**:
- Xử lý logic nghiệp vụ chính
- Điều phối giữa Presentation (qua Socket) và Data Layer (DAO)
- Quản lý transactions và tính nhất quán dữ liệu

**Các Service chính**:

| Service | Trách nhiệm |
|---------|-------------|
| `MessageHandlerService` | Điều phối tất cả các action từ client (LOGIN, BID, BUY_OUT...) |
| `UserService` | Xử lý đăng ký, đăng nhập |
| `BidService` | Xử lý logic đặt giá thầu và mua ngay |
| `SellService` | Xử lý đăng sản phẩm mới |
| `WinnerService` | Xác định người thắng cuộc |

### 2.3 Data Access Layer (Server)

**Vị trí**: `auction-server/src/main/java/auction_server/dao/`

**Trách nhiệm**:
- Giao tiếp trực tiếp với database
- Thực hiện các câu lệnh SQL (CRUD)
- Quản lý connection pool (HikariCP)

**Các DAO chính**:

| DAO | Bảng Database | Trách nhiệm |
|-----|---------------|-------------|
| `UserDAO` | `users` | CRUD user |
| `AuctionDAO` | `auctions` | CRUD auction, update highest bid |
| `BidTransactionDAO` | `bid_transactions` | Lưu lịch sử bid |
| `ItemDAO` | `items` | CRUD items |

### 2.4 Entity Layer

**Vị trí**: `auction-server/src/main/java/auction_server/entities/`

**Các Entity chính**:

| Entity | Mô tả |
|--------|-------|
| `User` | Thông tin người dùng |
| `Auction` | Phiên đấu giá |
| `Item` | Sản phẩm (abstract) |
| `BidTransaction` | Giao dịch đặt giá |

### 2.5 Data Transfer Object (DTO) Layer

**Vị trí**: `auction-shared/src/main/java/auction_shared/dto/`

**Mục đích**: Chuyển dữ liệu giữa Server và Client mà không chia sẻ Entity trực tiếp (đảm bảo tính đóng gói).

| DTO | Mô tả |
|-----|-------|
| `UserDTO` | Thông tin user (id, username, role) |
| `AuctionDTO` | Thông tin auction đầy đủ |
| `ItemDTO` | Thông tin sản phẩm |
| `BidTransactionDTO` | Thông tin giao dịch bid |
| `SignUpDTO` | Dùng cho login/register |
| `AuctionStatus` | Enum: ACTIVE, ENDED, SOLD |
| `ItemType` | Enum: ARTS, ELECTRONICS, VEHICLES |

---

## 3. Module Server (auction-server)

### 3.1 Khởi Động Server - Main.java

```java
1:  package auction_server;
2:  import java.util.List;
3:  import auction_server.Network.SocketServer;
4:  import auction_server.core.AuctionManager;
5:  import auction_server.core.AuctionScheduler;
6:  import auction_server.dao.AuctionDAO;
7:  import auction_server.dao.BidTransactionDAO;
8:  import auction_server.dao.DAOProvider;
9:  import auction_server.dao.DefaultDAOProvider;
10: import auction_server.entities.Auction;
11: import auction_server.entities.BidTransaction;
12:
13: public class Main {
14:     public static void main(String[] args) {
```

**Phân tích chi tiết `main()`**:

**Dòng 15-16**: Khởi tạo DAO Provider và các DAO cần thiết

```java
15:         DAOProvider daoProvider = new DefaultDAOProvider();
16:         AuctionDAO auctionDAO = daoProvider.auctionDAO();
17:         BidTransactionDAO bidDAO = daoProvider.bidTransactionDAO();
```

- `DefaultDAOProvider` là singleton implementation của `DAOProvider`
- Cung cấp các instance của `AuctionDAO`, `BidTransactionDAO`, `ItemDAO`, `UserDAO`

**Dòng 18-25**: Load các auction đang active từ database

```java
18:         AuctionManager manager = AuctionManager.getInstance();
19:         List<Auction> activeAuctions = auctionDAO.selectActiveAuctions();
20:         for (Auction auction : activeAuctions) {
21:             manager.addRoom(auction);
22:             List<BidTransaction> history = bidDAO.selectByAuctionId(auction.getAuctionId());
23:             auction.setBidHistory(history);
24:         }
25:         System.out.println("[System] Loaded " + activeAuctions.size() + " active auction(s) from database.");
```

**Giải thích luồng khởi động server**:
1. Lấy singleton `AuctionManager` - quản lý tất cả các phòng đấu giá đang hoạt động
2. Truy vấn database lấy các auction có `status = 'ACTIVE'`
3. Với mỗi auction:
   - Thêm vào `AuctionManager` (in-memory)
   - Load lịch sử bid từ `bid_transactions` table
   - Gán lại history vào auction để đảm bảo tính nhất quán

**Dòng 26-27**: Khởi động AuctionScheduler

```java
26:         AuctionScheduler scheduler = new AuctionScheduler(manager, daoProvider);
27:         scheduler.start();
```

- `AuctionScheduler` chạy một background thread kiểm tra các auction hết hạn mỗi giây
- Xử lý kết thúc auction, xác định người thắng

**Dòng 28-30**: Khởi động Socket Server

```java
28:         System.out.println("[System] Starting Socket Server...");
29:         new SocketServer().start(8080);
30:         System.out.println("Hello World");
31:     }
32: }
```

- Server lắng nghe tại port 8080
- Chấp nhận nhiều kết nối đồng thời sử dụng Virtual Threads (Java 21)

### 3.2 Network Layer - SocketServer.java

```java
1:  package auction_server.Network;
2:  import java.io.IOException;
3:  import java.net.ServerSocket;
4:  import java.net.Socket;
5:  import java.util.concurrent.ExecutorService;
6:  import java.util.concurrent.Executors;
7:  import org.slf4j.Logger;
8:  import org.slf4j.LoggerFactory;
9:  import auction_server.core.AuctionManager;
10: import auction_server.dao.DAOProvider;
11: import auction_server.dao.DefaultDAOProvider;
12:
13: public class SocketServer {
14:     private static final Logger log = LoggerFactory.getLogger(SocketServer.class);
15:     private final DAOProvider daoProvider = new DefaultDAOProvider();
```

**Giải thích các thành phần**:

| Thành phần | Ý nghĩa |
|------------|---------|
| `Logger` (SLF4J) | Ghi log thay vì System.out để dễ quản lý |
| `ExecutorService` | Thread pool để xử lý nhiều client đồng thời |
| `DAOProvider` | Cung cấp các DAO cho mỗi ClientHandler |

**Phương thức `start(int port)`** (dòng 16-35):

```java
16:     public void start(int port) {
17:         try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
18:              ServerSocket serverSocket = new ServerSocket(port)) {
19:             log.info("Server is available at port: {}", port);
20:             while (true) {
21:                 Socket clientSocket = serverSocket.accept();
22:                 log.info("A new client connected!");
23:                 ClientHandler clientHandler = new ClientHandler(clientSocket, daoProvider);
24:                 AuctionManager.getInstance().addClient(clientHandler);
25:                 executor.submit(clientHandler);
26:             }
27:         }
28:         catch (IOException e) {
29:             log.info("Server encountered an error", e);
30:         }
31:     }
32: }
```

**Chi tiết luồng hoạt động**:

1. **Dòng 17**: `Executors.newVirtualThreadPerTaskExecutor()` - Java 21 feature
   - Virtual threads nhẹ hơn OS threads truyền thống
   - Mỗi client được assign một virtual thread riêng
   - Có thể xử lý hàng nghìn concurrent connections

2. **Dòng 18**: Tạo `ServerSocket` tại port 8080
   - Dùng try-with-resources để đảm bảo đóng socket khi server shutdown

3. **Dòng 20-26**: Vòng lặp vô hạn chấp nhận clients
   - `serverSocket.accept()` - blocking, đợi client kết nối
   - Khi có client mới, tạo `ClientHandler` xử lý
   - Thêm handler vào `AuctionManager` để quản lý
   - Submit vào executor để xử lý song song

4. **Dòng 28-30**: Xử lý exception nếu server gặp lỗi

### 3.3 Network Layer - ClientHandler.java

```java
1:  package auction_server.Network;
2:  import java.io.IOException;
3:  import java.io.ObjectInputStream;
4:  import java.io.ObjectOutputStream;
5:  import java.net.Socket;
6:  import java.util.ArrayList;
7:  import java.util.List;
8:  import org.slf4j.Logger;
9:  import org.slf4j.LoggerFactory;
10: import auction_server.core.AuctionManager;
11: import auction_server.dao.DAOProvider;
12: import auction_server.entities.User;
13: import auction_server.service.MessageHandlerService;
14: import auction_shared.Network.NetworkMessage;
15: import auction_shared.Network.Notification;
16:
17: public class ClientHandler implements Runnable {
18:     private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
19:     private List<Notification> activities = new ArrayList<>();
20:     private MessageHandlerService messageHandler;
21:     private Socket socket;
22:     private ObjectOutputStream out;
23:     private ObjectInputStream in;
```

**Các thuộc tính của ClientHandler**:

| Thuộc tính | Kiểu | Mô tả |
|------------|------|--------|
| `activities` | `List<Notification>` | Danh sách thông báo hoạt động của user này |
| `messageHandler` | `MessageHandlerService` | Xử lý các business logic |
| `socket` | `Socket` | Socket kết nối với client |
| `out` | `ObjectOutputStream` | Stream gửi data về client |
| `in` | `ObjectInputStream` | Stream nhận data từ client |

**Constructor** (dòng 24-32):

```java
24:     public ClientHandler(Socket socket, DAOProvider daoProvider) {
25:         this.socket = socket;
26:         this.messageHandler = new MessageHandlerService(
27:                 activities,
28:                 this::sendMessage,
29:                 this::onLogout,
30:                 daoProvider
31:         );
32:     }
```

**Giải thích constructor injection**:
- `activities` - danh sách thông báo riêng của handler này
- `this::sendMessage` - method reference làm `MessageSender` callback
- `this::onLogout` - method reference làm `LogoutHandler` callback
- `daoProvider` - để truy cập database

**Phương thức `onLogout()`** (dòng 34-36):

```java
34:     private void onLogout() {
35:         AuctionManager.getInstance().removeClient(this);
36:     }
```

- Khi user logout, remove handler khỏi danh sách active clients
- Đảm bảo không gửi message đến client đã disconnect

**Phương thức `getLoggedInUser()`** (dòng 38-40):

```java
38:     public User getLoggedInUser() {
39:         return messageHandler.getLoggedInUser();
40:     }
```

- Delegate đến MessageHandlerService để lấy user hiện đang đăng nhập

**Phương thức `run()` - Main Loop** (dòng 42-57):

```java
42:     public void run() {
43:         try {
44:             out = new ObjectOutputStream(socket.getOutputStream());
45:             in = new ObjectInputStream(socket.getInputStream());
46:             while (true) {
47:                 NetworkMessage msg = (NetworkMessage) in.readObject();
48:                 try {
49:                     handleRequest(msg);
50:                 } catch (Exception e) {
51:                     log.error("Error handling request: {}", e.getMessage(), e);
52:                 }
53:             }
54:         } catch (Exception e) {
55:             AuctionManager.getInstance().removeClient(this);
56:             log.info("Client has disconnected");
57:         }
58:     }
```

**Chi tiết luồng run()**:

1. **Dòng 44-45**: Khởi tạo streams
   - **QUAN TRỌNG**: OutputStream khởi tạo TRƯỚC InputStream
   - Tránh deadlock do ObjectInputStream/ObjectOutputStream buffer conflict

2. **Dòng 46-53**: Vòng lặp nhận message
   - `in.readObject()` - blocking, đợi client gửi message
   - Cast về `NetworkMessage` (đảm bảo serialize/deserialize đúng)
   - `handleRequest()` xử lý message
   - Wrap trong try-catch để một lỗi không làm crash handler

3. **Dòng 54-57**: Xử lý disconnect
   - Khi có exception (VD: client đóng app), remove khỏi manager
   - Log thông báo client đã disconnect

**Phương thức `sendMessage()`** (dòng 60-68):

```java
60:     public synchronized void sendMessage(NetworkMessage msg) {
61:         try {
62:             out.writeObject(msg);
63:             out.flush();
64:             out.reset();
65:         } catch (IOException e) {
66:             log.info("fail to send message", e);
67:         }
68:     }
```

**Giải thích `sendMessage()`**:
- **`synchronized`**: Đảm bảo thread-safe khi nhiều thread cùng gọi (VD: AuctionScheduler broadcast)
- **`out.reset()`**: Xóa cache của ObjectOutputStream để object mới được serialize đúng
- Nếu gửi thất bại (client đóng), log lỗi nhưng không throw

**Phương thức `handleRequest()` - Router** (dòng 70-87):

```java
70:     private void handleRequest(NetworkMessage msg) {
71:         String action = msg.getAction();
72:         log.info("Handling request: {}", action);
73:         switch (action) {
74:             case "PLACE_BID": messageHandler.handlePlaceBid(msg); break;
75:             case "SELL": messageHandler.handleSell(msg); break;
76:             case "LOGIN": messageHandler.handleLogin(msg); break;
77:             case "GET_PRODUCTS": messageHandler.handleGetProducts(msg); break;
78:             case "BUY_OUT": messageHandler.handleBuyOut(msg); break;
79:             case "GET_MY_LIST": messageHandler.handleGetMyList(msg); break;
80:             case "CREATE_ACCOUNT": messageHandler.handleCreateAccount(msg); break;
81:             case "GET_ACTIVITIES": messageHandler.handleGetActivities(msg); break;
82:             case "BAN_USER": messageHandler.handleBanUser(msg); break;
83:             case "REMOVE_ITEM": messageHandler.handleRemoveItem(msg); break;
84:             case "GET_USERS": messageHandler.handleGetUsers(msg); break;
85:             case "GET_BID_HISTORY": messageHandler.handleGetBidHistory(msg); break;
86:             case "LOGOUT": messageHandler.handleLogout(msg); break;
87:             default: log.warn("Unknown action: {}", action); break;
88:         }
89:     }
```

**Bảng các Action và Handler tương ứng**:

| Action | Handler | Mô tả |
|--------|---------|-------|
| `LOGIN` | `handleLogin` | Xác thực user |
| `CREATE_ACCOUNT` | `handleCreateAccount` | Tạo tài khoản mới |
| `GET_PRODUCTS` | `handleGetProducts` | Lấy danh sách auction |
| `PLACE_BID` | `handlePlaceBid` | Đặt giá thầu |
| `BUY_OUT` | `handleBuyOut` | Mua ngay |
| `SELL` | `handleSell` | Đăng sản phẩm mới |
| `GET_MY_LIST` | `handleGetMyList` | Lấy sản phẩm của user |
| `GET_ACTIVITIES` | `handleGetActivities` | Lấy thông báo |
| `BAN_USER` | `handleBanUser` | Ban user (ADMIN) |
| `REMOVE_ITEM` | `handleRemoveItem` | Xóa sản phẩm |
| `GET_USERS` | `handleGetUsers` | Lấy danh sách users |
| `GET_BID_HISTORY` | `handleGetBidHistory` | Lấy lịch sử bid |
| `LOGOUT` | `handleLogout` | Đăng xuất |

### 3.4 Core Layer - AuctionManager.java

```java
1:  package auction_server.core;
2:  import auction_server.Network.ClientHandler;
3:  import auction_server.entities.Auction;
4:  import auction_shared.Network.NetworkMessage;
5:  import java.util.ArrayList;
6:  import java.util.List;
7:  import java.util.Map;
8:  import java.util.concurrent.ConcurrentHashMap;
9:  import java.util.concurrent.CopyOnWriteArrayList;
10:
11: public class AuctionManager {
12:     private static volatile AuctionManager manager = null;
13:     private final Map<String, Auction> activeRooms = new ConcurrentHashMap<>();
14:     private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
```

**Cấu trúc dữ liệu Thread-Safe**:

| Thuộc tính | Kiểu | Lý do chọn |
|------------|------|------------|
| `activeRooms` | `ConcurrentHashMap` | Nhiều thread đọc/ghi Map (Scheduler + ClientHandlers) |
| `activeClients` | `CopyOnWriteArrayList` | Duyệt đồng thời khi broadcast, ít thay đổi |

**Singleton Pattern với Double-Checked Locking** (dòng 16-21):

```java
16:     private AuctionManager() {}
17:
18:     public static synchronized AuctionManager getInstance() {
19:         if (manager != null) return manager;
20:         manager = new AuctionManager();
21:         return manager;
22:     }
```

**Giải thích Singleton**:
- `private constructor` - không cho phép khởi tạo từ bên ngoài
- `synchronized getInstance()` - đảm bảo thread-safe (có thể optimize thêm với double-check)
- `volatile manager` - đảm bảo visibility across threads

**Quản lý Rooms (Auctions)**:

```java
23:     public void addRoom(Auction auction) {
24:         activeRooms.put(auction.getItem().getId(), auction);
25:     }
```

- Key là `item.getId()` - đảm bảo unique vì mỗi item có một auction

```java
26:     public Auction getRoom(String itemId) {
27:         return activeRooms.get(itemId);
28:     }
```

- O(1) lookup theo itemId

```java
29:     public ArrayList<Auction> getAllRooms() {
30:         return new ArrayList<>(activeRooms.values());
31:     }
```

- Trả về snapshot (ArrayList copy) để tránh ConcurrentModificationException

```java
36:     public void removeRoom(Auction room) {
37:         activeRooms.remove(room.getItem().getId());
38:     }
```

**Quản lý Clients**:

```java
39:     public void addClient(ClientHandler client) {
40:         activeClients.add(client);
41:     }
42:
43:     public List<ClientHandler> getActiveClients() {
44:         return activeClients;
45:     }
46:
47:     public void removeClient(ClientHandler client) {
48:         activeClients.remove(client);
49:     }
```

**Broadcasting**:

```java
51:     public void broadCast(NetworkMessage msg) {
52:         for (ClientHandler client : activeClients) {
53:             client.sendMessage(msg);
54:         }
55:     }
```

- Gửi message đến TẤT CẢ clients đang kết nối
- VD: Khi có bid mới, thông báo tất cả clients cập nhật danh sách

### 3.5 Core Layer - AuctionScheduler.java

```java
1:  package auction_server.core;
2:  import java.io.Serializable;
3:  import java.util.List;
4:  import java.util.concurrent.Executors;
5:  import java.util.concurrent.ScheduledExecutorService;
6:  import java.util.concurrent.TimeUnit;
7:  import auction_server.Network.ClientHandler;
8:  import auction_server.dao.AuctionDAO;
9:  import auction_server.dao.DAOProvider;
10: import auction_server.entities.Auction;
11: import auction_server.entities.User;
12: import import auction_server.mapper.Mappers;
13: import auction_server.service.WinnerService;
14: import auction_shared.Network.NetworkMessage;
15: import auction_shared.dto.AuctionDTO;
16:
17: public class AuctionScheduler {
18:     private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
19:     private final AuctionManager auctionManager;
20:     private final AuctionDAO auctionDAO;
21:     private final WinnerService winnerService;
```

**Giải thích các thuộc tính**:

| Thuộc tính | Mô tả |
|-----------|-------|
| `scheduler` | Single-thread scheduler chạy task định kỳ |
| `auctionManager` | Tham chiếu đến AuctionManager để quản lý rooms |
| `auctionDAO` | Để cập nhật database khi auction kết thúc |
| `winnerService` | Xác định người thắng cuộc |

**Constructor** (dòng 23-28):

```java
23:     public AuctionScheduler(AuctionManager auctionManager, DAOProvider daoProvider) {
24:         this.auctionManager = auctionManager;
25:         this.auctionDAO = daoProvider.auctionDAO();
26:         this.winnerService = new WinnerService(daoProvider);
27:     }
```

**Phương thức `start()`** (dòng 30-32):

```java
30:     public void start() {
31:         scheduler.scheduleAtFixedRate(this::checkExpiredAuctions, 0, 1, TimeUnit.SECONDS);
32:     }
```

- `scheduleAtFixedRate(task, initialDelay, period, unit)`
- Chạy `checkExpiredAuctions()` ngay lần đầu (delay=0)
- Lặp lại mỗi 1 giây
- **Single thread** đảm bảo không có race condition

**Phương thức `checkExpiredAuctions()` - Heartbeat Logic** (dòng 34-61):

```java
34:     private void checkExpiredAuctions() {
35:         for (Auction auction : auctionManager.getAllRooms()) {
36:             if (auction.isExpired()) {
37:                 auction.endAuction();
38:                 String winnerId = winnerService.determineWinner(auction.getBidHistory());
39:                 auctionDAO.update(auction);
40:                 auctionManager.removeRoom(auction);
41:
42:                 AuctionDTO auctionDTO = Mappers.toDTO(auction);
43:                 if (winnerId != null) {
44:                     for (ClientHandler client : auctionManager.getActiveClients()) {
45:                         User u = client.getLoggedInUser();
46:                         if (u != null && winnerId.equals(u.getId())) {
47:                             client.sendMessage(new NetworkMessage("YOU_WON", auctionDTO));
48:                             break;
49:                         }
50:                     }
51:                 }
52:
53:                 auctionManager.broadCast(new NetworkMessage("AUCTION_ENDED", auctionDTO));
54:                 auctionManager.broadCast(new NetworkMessage("UPDATE_BID",
55:                         (Serializable) Mappers.toAuctionDTOList(auctionManager.getAllRooms())));
56:             }
57:         }
58:     }
```

**Chi tiết luồng xử lý auction hết hạn**:

1. **Dòng 35**: Duyệt qua tất cả active rooms (snapshot copy)
2. **Dòng 36**: Kiểm tra `isExpired()` - `LocalDateTime.now().isAfter(endTime)`
3. **Dòng 37**: Gọi `auction.endAuction()` - set status = ENDED, xác định winnerId
4. **Dòng 38**: Xác định winner cuối cùng (kiểm tra user không bị BANNED)
5. **Dòng 39**: Cập nhật database (status, winner_id)
6. **Dòng 40**: Remove khỏi in-memory manager
7. **Dòng 42-50**: Tìm và gửi thông báo "YOU_WON" cho người thắng
8. **Dòng 53**: Broadcast "AUCTION_ENDED" cho tất cả clients
9. **Dòng 54-55**: Broadcast "UPDATE_BID" với danh sách auction còn lại

**Phương thức `stop()`** (dòng 63-65):

```java
63:     public void stop() {
64:         scheduler.shutdown();
65:     }
```

- Graceful shutdown scheduler khi server tắt

### 3.6 Service Layer - MessageHandlerService.java

Đây là class quan trọng nhất, điều phối tất cả business logic.

**Cấu trúc** (dòng 1-55):

```java
1:  package auction_server.service;
2:  import java.io.Serializable;
3:  import java.time.LocalTime;
4:  import java.util.ArrayList;
5:  import java.util.List;
6:  import org.slf4j.Logger;
7:  import org.slf4j.LoggerFactory;
8:  import auction_server.Network.ClientHandler;
9:  import auction_server.core.AuctionManager;
10: import auction_server.dao.DAOProvider;
11: import auction_server.entities.Auction;
12: import auction_server.entities.BidTransaction;
13: import auction_server.entities.Item;
14: import auction_server.entities.User;
15: import auction_server.factory.ItemFactory;
16: import auction_server.mapper.Mappers;
17: import auction_shared.Network.NetworkMessage;
18: import import auction_shared.Network.Notification;
19: import auction_shared.dto.AuctionDTO;
20: import auction_shared.dto.BidTransactionDTO;
21: import import auction_shared.dto.ItemDTO;
22: import import auction_shared.dto.SignUpDTO;
23: import import auction_shared.dto.UserDTO;
24:
25: public class MessageHandlerService {
26:     private static final Logger log = LoggerFactory.getLogger(MessageHandlerService.class);
27:     private final UserService userService;
28:     private final SellService sellService;
29:     private final DAOProvider daoProvider;
30:     private final List<Notification> activities;
31:     private User loggedInUser;
32:     private final MessageSender messageSender;
33:     private final LogoutHandler logoutHandler;
34:
35:     public interface MessageSender {
36:         void sendMessage(NetworkMessage msg);
37:     }
38:
39:     public interface LogoutHandler {
40:         void onLogout();
41:     }
```

**Callback Interfaces**:

| Interface | Mục đích |
|-----------|----------|
| `MessageSender` | Gửi message về cho client (implementation: `ClientHandler::sendMessage`) |
| `LogoutHandler` | Xử lý logout (implementation: `ClientHandler::onLogout`) |

**Constructor** (dòng 43-51):

```java
43:     public MessageHandlerService(List<Notification> activities, MessageSender messageSender,
44:                                 LogoutHandler logoutHandler, DAOProvider daoProvider) {
45:         this.daoProvider = daoProvider;
46:         this.userService = new UserService(daoProvider);
47:         this.sellService = new SellService(daoProvider);
48:         this.activities = activities;
49:         this.messageSender = messageSender;
50:         this.logoutHandler = logoutHandler;
51:     }
```

**Các Handler Methods chi tiết**:

#### 3.6.1 handlePlaceBid - Đặt giá thầu

```java
53:     public void handlePlaceBid(NetworkMessage msg) {
54:         BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();
55:         Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
56:
57:         if (auction == null) {
58:             messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
59:             activities.add(new Notification("bid failed: auction not found", LocalTime.now()));
60:             return;
61:         }
62:
63:         User bidder = daoProvider.userDAO().getUserByUsername(transactionDTO.getBidder().getUsername());
64:         if (bidder == null) {
65:             messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
66:             activities.add(new Notification("bid failed: user not found", LocalTime.now()));
67:             return;
68:         }
69:
70:         BidTransaction transaction = new BidTransaction(auction, bidder, transactionDTO.getBidAmount());
71:         BidService bidService = new BidService(daoProvider);
72:         boolean isSuccess = bidService.processAndSaveBid(auction, transaction);
73:
74:         if (isSuccess) {
75:             messageSender.sendMessage(new NetworkMessage("BID_SUCCESS", Mappers.toDTO(auction)));
76:             AuctionManager.getInstance().broadCast(new NetworkMessage("UPDATE_BID",
77:                     (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())));
78:             log.info("A new bid has been placed");
79:             activities.add(new Notification("you have placed bid successfully", LocalTime.now()));
80:         } else {
81:             log.info("Your bid has failed");
82:             messageSender.sendMessage(new NetworkMessage("BID_FAILED", null));
83:             activities.add(new Notification("Your bid has failed", LocalTime.now()));
84:         }
85:     }
```

**Luồng xử lý place bid**:
1. Parse `BidTransactionDTO` từ message
2. Tìm auction trong `AuctionManager` theo itemId
3. Validate: auction tồn tại, user tồn tại
4. Tạo `BidTransaction` với auction, bidder, bidAmount
5. Gọi `BidService.processAndSaveBid()` - xử lý logic + lưu DB
6. Nếu thành công: gửi `BID_SUCCESS` cho bidder + broadcast `UPDATE_BID`
7. Ghi log activity

#### 3.6.2 handleSell - Đăng sản phẩm mới

```java
87:     public void handleSell(NetworkMessage msg) {
88:         AuctionDTO auctionDTO = (AuctionDTO) msg.getData();
89:         ItemDTO itemDTO = auctionDTO.getItem();
90:
91:         // Lấy owner từ database
92:         User owner = daoProvider.userDAO().getUserByUsername(itemDTO.getOwner().getUsername());
93:
94:         // Factory pattern: tạo Item theo type (Arts/Electronics/Vehicles)
95:         Item item = ItemFactory.of(itemDTO.getType()).create(
96:                 itemDTO.getId(), itemDTO.getName(), itemDTO.getDescription(), owner);
97:
98:         // Tạo Auction entity
99:         Auction room = new Auction(item, auctionDTO.getStartingPrice(), auctionDTO.getBuyOutPrice(),
100:                auctionDTO.getTickSize(), auctionDTO.getStartTime(), auctionDTO.getEndTime(),
101:                auctionDTO.isAntiSniping());
102:
103:         boolean isSuccess = sellService.publishItemAndAuction(item, room);
104:         if (isSuccess) {
105:             AuctionManager.getInstance().addRoom(room);
106:             AuctionManager.getInstance().broadCast(new NetworkMessage("UPDATE_BID",
107:                     (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())));
108:             messageSender.sendMessage(new NetworkMessage("SELL_SUCCESS", true));
109:             log.info("SELL SUCCESS");
110:             activities.add(new Notification("you have sold item successfully", LocalTime.now()));
111:         } else {
112:             messageSender.sendMessage(new NetworkMessage("SELL_FAILED", false));
113:             log.info("SELL FAIL");
114:             activities.add(new Notification("sell item failed", LocalTime.now()));
115:         }
116:     }
```

**Factory Pattern ở đây**:
- `ItemFactory.of(ItemType.ARTS)` trả về `ArtsFactory`
- `ArtsFactory.create()` tạo instance của `Arts`
- Tương tự cho Electronics, Vehicles

#### 3.6.3 handleBuyOut - Mua ngay

```java
118:     public void handleBuyOut(NetworkMessage msg) {
119:         BidTransactionDTO transactionDTO = (BidTransactionDTO) msg.getData();
120:         Auction auction = AuctionManager.getInstance().getRoom(transactionDTO.getAuction().getItem().getId());
121:
122:         if (auction == null) {
123:             messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", null));
124:             return;
125:         }
126:
127:         User bidder = daoProvider.userDAO().getUserByUsername(transactionDTO.getBidder().getUsername());
128:         BidTransaction transaction = new BidTransaction(auction, bidder, transactionDTO.getBidAmount());
129:         BidService bidService = new BidService(daoProvider);
130:         boolean isSuccess = bidService.processBuyOut(auction, transaction);
131:
132:         if (isSuccess) {
133:             AuctionManager.getInstance().removeRoom(auction);
134:             AuctionDTO auctionDTO = Mappers.toDTO(auction);
135:             String winnerId = auction.getWinnerId();
136:
137:             if (winnerId != null) {
138:                 for (ClientHandler client : AuctionManager.getInstance().getActiveClients()) {
139:                     User u = client.getLoggedInUser();
140:                     if (u != null && winnerId.equals(u.getId())) {
141:                         client.sendMessage(new NetworkMessage("YOU_WON", auctionDTO));
142:                         break;
143:                     }
144: }
145:             }
146:             AuctionManager.getInstance().broadCast(new NetworkMessage("AUCTION_ENDED", auctionDTO));
147:             AuctionManager.getInstance().broadCast(new NetworkMessage("UPDATE_BID",
148:                     (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())));
149:             messageSender.sendMessage(new NetworkMessage("BUYOUT_SUCCESS", null));
150:             log.info("BUY OUT SUCCESS");
151:             activities.add(new Notification("you have buy out item successfully", LocalTime.now()));
152:         } else {
153:             messageSender.sendMessage(new NetworkMessage("BUYOUT_FAILED", null));
154:         }
155:     }
```

**Điểm khác biệt với placeBid**:
- `auction.removeRoom()` vì auction kết thúc ngay khi buy-out thành công
- Status = SOLD thay vì ACTIVE
- Không có anti-sniping extension (buy-out không bị snipe)

#### 3.6.4 handleLogin - Đăng nhập

```java
157:     public void handleLogin(NetworkMessage msg) {
158:         SignUpDTO dto = (SignUpDTO) msg.getData();
159:         User user = userService.login(dto.getUsername(), dto.getPassword());
160:         boolean isSuccess = user != null;
161:         if (isSuccess) this.loggedInUser = user;
162:         messageSender.sendMessage(new NetworkMessage("LOGIN", Mappers.toDTO(user)));
163:         log.info("{}{}", dto.getUsername(), isSuccess ? " successfully login" : " failed to login");
164:         activities.add(new Notification(isSuccess ? "login successfully" : "login failed", LocalTime.now()));
165:     }
```

**Lưu ý**: User entity được chuyển thành UserDTO trước khi gửi về client (không gửi password)

#### 3.6.5 handleGetProducts - Lấy danh sách sản phẩm

```java
167:     public void handleGetProducts(NetworkMessage msg) {
168:         messageSender.sendMessage(new NetworkMessage("GET_PRODUCTS",
169:                 (Serializable) Mappers.toAuctionDTOList(AuctionManager.getInstance().getAllRooms())));
170:     }
```

**Đặc điểm**:
- Không có validation - ai cũng có thể xem sản phẩm
- Trả về danh sách AuctionDTO (đã được map từ Auction entity)

#### 3.6.6 handleLogout - Đăng xuất

```java
300:     public void handleLogout(NetworkMessage msg) {
301:         this.loggedInUser = null;
302:         activities.clear();
303:         if (logoutHandler != null) logoutHandler.onLogout();
304:     }
```

**Hành động khi logout**:
1. Xóa loggedInUser
2. Clear danh sách activities
3. Gọi `logoutHandler.onLogout()` - remove khỏi AuctionManager

### 3.7 Service Layer - BidService.java

```java
1:  package auction_server.service;
2:  import java.sql.Connection;
3:  import java.sql.SQLException;
4:  import java.time.LocalDateTime;
5:  import org.slf4j.Logger;
6:  import org.slf4j.LoggerFactory;
7:  import auction_server.dao.AuctionDAO;
8:  import auction_server.dao.BidTransactionDAO;
9:  import auction_server.dao.DAOProvider;
10: import auction_server.dao.DatabaseConnection;
11: import auction_server.entities.Auction;
12: import auction_server.entities.BidTransaction;
13:
14: public class BidService {
15:     private static final Logger log = LoggerFactory.getLogger(BidService.class);
16:     private final AuctionDAO auctionDAO;
17:     private final BidTransactionDAO bidDAO;
18:
19:     public BidService(DAOProvider daoProvider) {
20:         this.auctionDAO = daoProvider.auctionDAO();
21:         this.bidDAO = daoProvider.bidTransactionDAO();
22:     }
```

**Constructor**: Nhận DAOProvider, lấy các DAO cần thiết

**Phương thức `findWinnerId()`** (dòng 24-30):

```java
24:     public String findWinnerId(String auctionId) {
25:         BidTransaction topTx = bidDAO.findTopBidderByAuction(auctionId);
26:         if (topTx != null && topTx.getBidder() != null) {
27:             return topTx.getBidder().getId();
28:         }
29:         return null;
30:     }
```

**Phương thức `processAndSaveBid()` - Core Logic** (dòng 32-58):

```java
32:     public boolean processAndSaveBid(Auction auction, BidTransaction transaction) {
33:         // 1. Gọi auction.placeBid() để validate và update in-memory state
34:         if (!auction.placeBid(transaction)) return false;
35:
36:         // 2. Bắt đầu database transaction
37:         try (Connection conn = DatabaseConnection.getConnection()) {
38:             conn.setAutoCommit(false);
39:             try {
40:                 // 3. Insert bid transaction vào DB
41:                 bidDAO.insert(transaction, conn);
41:
42:                 // 4. Update current_highest_bid trong auctions table
43:                 auctionDAO.updateHighestBid(auction, conn);
44:
45:                 // 5. Commit transaction
46:                 conn.commit();
47:
48:                 // 6. Nếu có anti-sniping, extend time nếu cần
49:                 if (auction.isAntiSniping()) {
50:                     LocalDateTime oldEndTime = auction.getEndTime();
51:                     auction.extendTime();
52:                     if (!oldEndTime.equals(auction.getEndTime())) {
53:                         auctionDAO.updateEndTime(auction, conn);
54:                     }
55:                 }
56:
57:                 log.info("Bid thành công: Auction={}, Bidder={}, BidAmount={}",
58:                         auction.getAuctionId(), transaction.getBidder().getUsername(), transaction.getBidAmount());
59:                 return true;
60:             } catch (SQLException e) {
61:                 // 7. Rollback nếu có lỗi
62:                 conn.rollback();
63:                 log.error("Lỗi Transaction DB khi lưu Bid, đang rollback cả DB và RAM...", e);
64:                 auction.revertLastBid(transaction);
65:                 return false;
66:             }
67:         } catch (SQLException e) {
68:             log.error("Không thể lấy Connection DB", e);
69:             auction.revertLastBid(transaction);
70:             return false;
71:         }
72:     }
```

**Chi tiết luồng xử lý bid với Transaction Safety**:

```
┌──────────────────────────────────────────────────────────────────────┐
│                    processAndSaveBid() Flow                          │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. auction.placeBid(transaction)                                    │
│     ├─ Lock ReentrantLock                                            │
│     ├─ Validate: status, expiration, owner, bidAmount, tickSize     │
│     ├─ Update: bidHistory.add(), currentHighestBid = bidAmount       │
│     └─ Unlock                                                       │
│     │                                                               │
│     ▼ (nếu placeBid() return false → reject ngay)                  │
│                                                                      │
│  2. Database Transaction                                             │
│     │                                                               │
│     ├─ conn.setAutoCommit(false)                                    │
│     │                                                               │
│     ├─ bidDAO.insert(transaction, conn)                             │
│     │   └─ INSERT INTO bid_transactions...                          │
│     │                                                               │
│     ├─ auctionDAO.updateHighestBid(auction, conn)                   │
│     │   └─ UPDATE auctions SET current_highest_bid = ?             │
│     │                                                               │
│     ├─ conn.commit()                                                │
│     │   └─ THÀNH CÔNG ──────────────────────────────────────┐      │
│     │                                                            │      │
│     │   THẤT BẠI ◄────────────────────────────────────────────┘      │
│     │   └─ conn.rollback()                                          │
│     │   └─ auction.revertLastBid(transaction) ← RAM state rollback │
│     │                                                               │
│     ▼ (nếu có anti-sniping)                                         │
│                                                                      │
│  3. Anti-Sniping Extension                                          │
│     ├─ oldEndTime = auction.getEndTime()                            │
│     ├─ auction.extendTime()                                          │
│     │   └─ Nếu remaining <= 30s: endTime += 30s                    │
│     └─ auctionDAO.updateEndTime(auction, conn) ← sync DB             │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**Phương thức `processBuyOut()`** (dòng 74-91):

```java
74:     public boolean processBuyOut(Auction auction, BidTransaction transaction) {
75:         if (!auction.buyOut(transaction)) return false;
76:         try (Connection conn = DatabaseConnection.getConnection()) {
77:             conn.setAutoCommit(false);
78:             try {
79:                 bidDAO.insert(transaction, conn);
80:                 auctionDAO.updateStatusAndWinner(auction, conn);
81:                 conn.commit();
82:                 log.info("Buy Out thành công: Auction={}, Winner={}",
83:                         auction.getAuctionId(), auction.getWinnerId());
84:                 return true;
85:             } catch (SQLException e) {
86:                 conn.rollback();
87:                 log.error("Lỗi Transaction DB khi Buy Out, đang rollback cả DB và RAM...", e);
88:                 auction.revertBuyOut(transaction);
89:                 return false;
90:             }
91:         } catch (SQLException e) {
92:             log.error("Không thể lấy Connection DB cho Buy Out", e);
93:             auction.revertBuyOut(transaction);
94:             return false;
95:         }
96:     }
```

### 3.8 Service Layer - UserService.java

```java
1:  package auction_server.service;
2:  import auction_server.dao.DAOProvider;
3:  import auction_server.dao.UserDAO;
4:  import auction_server.entities.User;
5:
6:  public class UserService {
7:     private final UserDAO userDAO;
8:
9:     public UserService(DAOProvider daoProvider) {
10:         this.userDAO = daoProvider.userDAO();
11:     }
12:
13:     public boolean register(User user) {
14:         if (userDAO.getUserByUsername(user.getUsername()) != null) return false;
15:         return userDAO.insertUser(user);
16:     }
17:
18:     public User login(String username, String password) {
19:         User user = userDAO.getUserByUsername(username);
20:         if (user != null && user.getPassword().equals(password) && !user.getUserStatus().equals("BANNED")) {
21:             return user;
22:         }
23:         return null;
24:     }
25: }
```

**Luồng register**:
1. Kiểm tra username đã tồn tại chưa
2. Nếu chưa, insert user vào database

**Luồng login**:
1. Tìm user theo username
2. Validate password khớp
3. Validate user không bị BANNED
4. Trả về User entity nếu thành công

### 3.9 Service Layer - SellService.java

```java
1:  package auction_server.service;
2:  import auction_server.dao.DAOProvider;
3:  import auction_server.dao.ItemDAO;
4:  import auction_server.entities.Auction;
5:  import auction_server.entities.Item;
6:
7:  public class SellService {
8:     private final ItemDAO itemDAO;
9:     private final auction_server.dao.AuctionDAO auctionDAO;
10:
11:     public SellService(DAOProvider daoProvider) {
12:         this.itemDAO = daoProvider.itemDAO();
13:         this.auctionDAO = daoProvider.auctionDAO();
14:     }
15:
16:     public boolean publishItemAndAuction(Item item, Auction auction) {
17:         int itemResult = itemDAO.insert(item);
18:         int auctionResult = auctionDAO.insert(auction);
19:         return itemResult > 0 && auctionResult > 0;
20:     }
21: }
```

**Lưu ý**: Không có transaction ở đây - nếu item insert thành công nhưng auction insert thất bại, sẽ có inconsistency (item tồn tại không có auction)

### 3.10 Service Layer - WinnerService.java

```java
1:  package auction_server.service;
2:  import auction_server.dao.DAOProvider;
3:  import auction_server.dao.UserDAO;
4:  import auction_server.entities.BidTransaction;
5:  import auction_server.entities.User;
6:  import java.util.List;
7:
8:  public class WinnerService {
9:     private final UserDAO userDAO;
10:
11:     public WinnerService(DAOProvider daoProvider) {
12:         this.userDAO = daoProvider.userDAO();
13:     }
14:
15:     public String determineWinner(List<BidTransaction> bidHistory) {
16:         if (bidHistory == null || bidHistory.isEmpty()) return null;
17:         for (int i = bidHistory.size() - 1; i > 0; i--) {
18:             BidTransaction tx = bidHistory.get(i);
19:             User bidder = tx.getBidder();
20:             if (bidder == null) continue;
21:             User user = userDAO.getUserByUsername(bidder.getUsername());
22:             if (user != null && !"BANNED".equals(user.getUserStatus())) {
23:                 return bidder.getId();
24:             }
25:         }
26:         return null;
27:     }
28: }
```

**Luồng xác định người thắng**:
1. Duyệt từ cuối list (bid gần nhất)
2. Với mỗi bid, kiểm tra user có tồn tại và không bị BANNED
3. Trả về ID của user đầu tiên hợp lệ
4. Nếu tất cả bidders đều bị BANNED hoặc list rỗng → return null

**Tại sao duyệt ngược?**
- Bid gần nhất có bidAmount cao nhất (do validation trong `placeBid`)
- Nhưng người bid gần nhất có thể bị BANNED sau đó
- Cần kiểm tra từ gần nhất ra để tìm người hợp lệ cao nhất

### 3.11 Entity Layer - Auction.java

Đây là entity phức tạp nhất, chứa tất cả business logic của một phiên đấu giá.

**Các hằng số** (dòng 13-14):

```java
13:     private static final long SNIPING_GRACE_SECONDS = 30;  // 30 giây cuối
14:     private static final long EXTENSION_SECONDS = 30;       // Thêm 30 giây
```

**Cấu trúc dữ liệu** (dòng 17-34):

```java
17:     private String auctionId;          // = item.getId()
18:     private Item item;                // Sản phẩm đấu giá
19:     private double startingPrice;     // Giá khởi điểm
20:     private double buyOutPrice;       // Giá mua ngay
21:     private double tickSize;          // Bước giá tối thiểu
22:     private LocalDateTime startTime;
23:     private LocalDateTime endTime;
24:     private boolean antiSniping;      // Có anti-snipe không
25:     private double currentHighestBid;
26:     private AuctionStatus status;     // ACTIVE / ENDED / SOLD
27:     private String winnerId;
28:     private final List<BidTransaction> bidHistory = new ArrayList<>();
29:     private final ReentrantLock lock = new ReentrantLock();
30:     private transient User originalOwnerBeforeBuyOut;
```

**Constructors** (dòng 36-68):

```java
36:     public Auction(Item item, double startingPrice, double buyOutPrice, double tickSize,
37:             LocalDateTime startTime, LocalDateTime endTime, boolean antiSniping) {
38:         this.status = AuctionStatus.ACTIVE;
39:         this.auctionId = item.getId();
40:         // ... gán các giá trị
41:     }
```

Constructor 1: Dùng khi tạo auction mới từ client

```java
54:     public Auction(Item item, double startingPrice, double buyOutPrice, double tickSize,
55:                   LocalDateTime startTime, LocalDateTime endTime, boolean antiSniping,
56:                   double currentHighestBid, AuctionStatus status) {
57:         // ...
58:     }
```

Constructor 2: Dùng khi rebuild từ database (server restart)

**Phương thức `placeBid()`** (dòng 124-163):

```java
124:     public boolean placeBid(BidTransaction transaction) {
125:         lock.lock();
126:         try {
127:             // Check 1: Auction phải đang ACTIVE
128:             if (status != AuctionStatus.ACTIVE) return false;
129:
130:             // Check 2: Nếu đã hết hạn thì end auction luôn
131:             if (isExpired()) { endAuction(); return false; }
132:
133:             double bidAmount = transaction.getBidAmount();
134:
135:             // Check 3: Owner không được bid sản phẩm của mình
136:             if (getItem().getOwner().getUsername().equals(transaction.getBidder().getUsername())) {
137:                 return false;
138:             }
139:
140:             // Check 4: Bid phải lớn hơn giá hiện tại
141:             if (bidAmount <= getCurrentHighestBid()) return false;
142:
143:             // Check 5: Bid phải nhỏ hơn buyOutPrice (nếu dùng buy-out)
144:             if (bidAmount >= buyOutPrice) return false;
145:
146:             // Check 6: Bid increment phải là bội số của tickSize
147:             double increment = bidAmount - getCurrentHighestBid();
148:             long ticks = Math.round(increment / tickSize);
149:             if (ticks <= 0 || Math.abs(increment - ticks * tickSize) > 0.001) {
150:                 return false;
151:             }
152:
153:             // Thành công: update state
154:             addTransaction(transaction);
155:             setCurrentHighestBid(transaction.getBidAmount());
156:             return true;
157:         } finally {
158:             lock.unlock();
159:         }
160:     }
```

**Giải thích các validation**:

| Check | Điều kiện | Lý do |
|-------|----------|-------|
| 1 | `status == ACTIVE` | Auction đã ENDED/SOLD thì không bid được |
| 2 | `!isExpired()` | Auction hết hạn thì không bid được |
| 3 | Owner ≠ Bidder | Chủ sở hữu không thể tự bid sản phẩm mình |
| 4 | `bidAmount > currentHighestBid` | Bid phải cao hơn giá hiện tại |
| 5 | `bidAmount < buyOutPrice` | Nếu muốn bid = buyOutPrice, dùng luồng BUY_OUT |
| 6 | `increment % tickSize == 0` | Bước giá phải đúng tickSize |

**Phương thức `buyOut()`** (dòng 165-195):

```java
165:     public boolean buyOut(BidTransaction transaction) {
166:         lock.lock();
167:         try {
168:             if (status != AuctionStatus.ACTIVE) return false;
169:             if (isExpired()) { endAuction(); return false; }
170:
171:             // Owner không được buy-out sản phẩm của mình
172:             if (transaction.getBidder().getUsername().equals(getItem().getOwner().getUsername())) {
173:                 return false;
174:             }
175:
176:             // Bid amount phải đúng bằng buyOutPrice
177:             if (Math.abs(transaction.getBidAmount() - buyOutPrice) > 0.001) {
178:                 return false;
179:             }
180:
181:             // Lưu owner cũ để revert nếu DB lỗi
182:             this.originalOwnerBeforeBuyOut = item.getOwner();
183:
184:             // Update state: chuyển ownership
185:             item.setOwner(transaction.getBidder());
186:             status = AuctionStatus.SOLD;
187:             winnerId = transaction.getBidder().getId();
188:             return true;
189:         } finally {
190:             lock.unlock();
191:         }
192:     }
```

**Điểm khác biệt với placeBid**:
- Không có tickSize validation (chỉ cần đúng buyOutPrice)
- Cập nhật `item.owner` (chuyển quyền sở hữu)
- Status = SOLD thay vì ACTIVE
- Lưu `originalOwnerBeforeBuyOut` để revert nếu cần

**Phương thức `extendTime()` - Anti-Sniping** (dòng 110-122):

```java
110:     public void extendTime() {
111:         lock.lock();
112:         try {
113:             if (status != AuctionStatus.ACTIVE) return;
114:
115:             long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
116:             if (remaining <= SNIPING_GRACE_SECONDS && remaining > 0) {
117:                 endTime = endTime.plusSeconds(EXTENSION_SECONDS);
118:             }
119:         } finally {
120:             lock.unlock();
121:         }
122:     }
```

**Luồng anti-sniping**:
1. Tính remaining time
2. Nếu ≤ 30 giây VÀ > 0 → thêm 30 giây
3. Nếu ≤ 0 (đã hết hạn) → không làm gì

**Phương thức `endAuction()`** (dòng 91-108):

```java
91:     public void endAuction() {
92:         lock.lock();
93:         try {
94:             if (status != AuctionStatus.ACTIVE) return;
95:             status = AuctionStatus.ENDED;
96:             if (!bidHistory.isEmpty()) {
97:                 for (int i = bidHistory.size() - 1; i >= 0; i--) {
98:                     if (bidHistory.get(i).getBidder() != null) {
99:                         winnerId = bidHistory.get(i).getBidder().getId();
100:                        break;
101:                     }
102:                 }
103:             }
104:         } finally {
105:             lock.unlock();
106:         }
107:     }
```

**Xác định winner**:
1. Set status = ENDED
2. Duyệt bidHistory từ cuối lên (bid cao nhất)
3. Gán winnerId cho bidder đầu tiên có non-null bidder

### 3.12 DAO Layer - Chi Tiết

#### 3.12.1 DAOProvider Interface

```java
package auction_server.dao;
public interface DAOProvider {
    AuctionDAO auctionDAO();
    BidTransactionDAO bidTransactionDAO();
    ItemDAO itemDAO();
    UserDAO userDAO();
}
```

**Factory Pattern**: Interface này định nghĩa contract, `DefaultDAOProvider` cung cấp implementation.

#### 3.12.2 DefaultDAOProvider

```java
package auction_server.dao;
public class DefaultDAOProvider implements DAOProvider {
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override public AuctionDAO auctionDAO() { return auctionDAO; }
    // ... các method khác tương tự
}
```

**Singleton pattern** (lazy initialization): Các DAO được khởi tạo khi DefaultDAOProvider được tạo và tái sử dụng.

#### 3.12.3 DatabaseConnection (HikariCP)

```java
package auction_server.dao;
public class DatabaseConnection {
    private static HikariDataSource dataSource = null;

    private static synchronized void init() {
        if (dataSource != null) return;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USER"));
        config.setPassword(System.getenv("DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) init();
        return dataSource.getConnection();
    }
}
```

**HikariCP Connection Pool**:
- **Maximum Pool Size = 10**: Tối đa 10 connections đồng thời
- **Lazy initialization**: Pool chỉ được tạo khi lần đầu gọi `getConnection()`
- **Singleton**: Chỉ một pool instance cho toàn bộ ứng dụng
- **Environment Variables**: DB credentials từ `DB_URL`, `DB_USER`, `DB_PASSWORD`

#### 3.12.4 AuctionDAO

```java
public class AuctionDAO implements WritableDAO<Auction> {

    @Override
    public int insert(Auction auction) {
        String sql = "INSERT INTO auctions (id, item_id, starting_price, buy_out_price, tick_size, current_highest_bid, start_time, end_time, auction_status, anti_snipe) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getItem().getId());
            // ... set các tham số
            return pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); return 0; }
    }

    public List<Auction> selectActiveAuctions() {
        // JOIN 3 bảng: auctions, items, users
        String sql = "SELECT a.*, i.id as item_id, i.item_name, i.description, i.item_type, " +
                     "u.id as owner_id, u.username, u.password " +
                     "FROM auctions a " +
                     "JOIN items i ON a.item_id = i.id " +
                     "JOIN users u ON i.owner_id = u.id " +
                     "WHERE a.auction_status = 'ACTIVE'";
        // ...
    }

    private Item mapRowToItem(ResultSet rs, User owner) throws SQLException {
        ItemType type = ItemType.fromDbValue(rs.getString("item_type"));
        return switch (type) {
            case ARTS -> new Arts(id, name, description, owner);
            case ELECTRONICS -> new Electronics(id, name, description, owner);
            case VEHICLES -> new Vehicles(id, name, description, owner);
        };
    }

    @Override
    public int update(Auction auction) {
        String sql = "UPDATE auctions SET auction_status = ?, winner_id = ?, current_highest_bid = ? WHERE id = ?";
        // ...
    }
}
```

**Join 3 bảng để rebuild Auction entity**:
- `auctions`: Thông tin giá, thời gian, status
- `items`: Thông tin sản phẩm (name, description, type)
- `users`: Thông tin owner

#### 3.12.5 BidTransactionDAO

```java
public class BidTransactionDAO implements TransactionalDAO<BidTransaction> {

    @Override
    public int insert(BidTransaction bt, Connection conn) throws SQLException {
        String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, bid_time) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bt.getId());
            ps.setString(2, bt.getAuction().getAuctionId());
            ps.setString(3, bt.getBidder().getId());
            ps.setDouble(4, bt.getBidAmount());
            ps.setTimestamp(5, Timestamp.valueOf(bt.getBidTime()));
            return ps.executeUpdate();
        }
    }

    public BidTransaction findTopBidderByAuction(String auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";
        // Trả về bid có bid_amount cao nhất
    }

    public ArrayList<BidTransaction> selectByAuctionId(String auctionId) {
        String sql = "SELECT bt.*, u.username, u.user_status " +
                     "FROM bid_transactions bt " +
                     "JOIN users u ON bt.bidder_id = u.id " +
                     "WHERE bt.auction_id = ? ORDER BY bt.bid_time ASC";
        // Trả về tất cả bids theo thứ tự thời gian
    }
}
```

**Lưu ý**:
- `insert()` nhận Connection parameter cho transaction support
- JOIN với users để lấy username (cho việc display)
- `ORDER BY bid_amount DESC LIMIT 1` cho findTopBidderByAuction

#### 3.12.6 UserDAO

```java
public class UserDAO {

    public boolean insertUser(User user) {
        String sql = "INSERT INTO users (id, username, password, role, user_status) VALUES (?, ?, ?, ?, ?)";
        // ...
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        // ...
    }

    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users WHERE role = 'USER' AND user_status = 'AVAILABLE'";
        // Chỉ lấy USER không bị BANNED
    }

    public boolean userBan(UserDTO user) {
        String sql = "UPDATE users SET user_status = 'BANNED' WHERE id = ?";
        // ...
    }
}
```

### 3.13 Mappers

**Mappers** chuyển đổi giữa Entity (server-side) và DTO (shared), đảm bảo:
1. Không expose server internals ra client
2. Chỉ gửi những dữ liệu cần thiết
3. Tránh serialize password

```java
public static UserDTO toDTO(User user) {
    if (user == null) return null;
    return new UserDTO(user.getId(), user.getUsername(), user.getRole());
    // Không bao gồm password!
}
```

---

## 4. Module Client (auction-client)

### 4.1 Kiểu File và Cấu Trúc

```
auction-client/
├── src/main/java/auction_client/
│   ├── launcher/
│   │   ├── Launcher.java          # Entry point
│   │   └── ClientLauncher.java    # JavaFX Application
│   ├── Network/
│   │   └── ClientService.java     # Socket client singleton
│   ├── controllers/
│   │   ├── SignInController.java
│   │   ├── WebMenuBarController.java
│   │   ├── BidProductSceneController.java
│   │   ├── SellProductSceneController.java
│   │   ├── FilteredProductSceneController.java
│   │   └── ... (các controller khác)
│   └── interfaces/
│       └── AuctionUpdateListener.java
├── src/main/resources/auction_client/
│   ├── SignInScene.fxml
│   ├── SignUpScene.fxml
│   ├── AuctionMain.fxml
│   └── ... (các FXML scenes)
```

### 4.2 Launcher - Điểm Khởi Đầu

```java
package auction_client.launcher;

import auction_client.Network.ClientService;
import auction_client.controllers.notification.UserPushUpNotificationController;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        try {
            ClientService clientService = ClientService.getInstance();
            String host = "localhost";
            int port = 8080;
            clientService.connect(host, port);
            System.out.println("Connected to server successfully!");

            // Đăng ký global notification listener
            clientService.addListener(new UserPushUpNotificationController());
        } catch (Exception e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }

        Application.launch(ClientLauncher.class, args);
    }
}
```

**Luồng khởi động client**:
1. Lấy singleton `ClientService`
2. Kết nối đến server tại `localhost:8080`
3. Đăng ký `UserPushUpNotificationController` để nhận push notifications
4. Launch JavaFX Application

### 4.3 ClientService - Singleton Socket Client

```java
package auction_client.Network;

public class ClientService {
    private static ClientService instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false;
    private final List<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();
}
```

**Các thuộc tính**:

| Thuộc tính | Mô tả |
|-----------|-------|
| `instance` | Singleton instance |
| `socket` | Socket kết nối đến server |
| `out` | Stream gửi data |
| `in` | Stream nhận data |
| `isRunning` | Trạng thái kết nối |
| `listeners` | Danh sách observers (CopyOnWriteArrayList for thread-safety) |

**Phương thức `connect()`** (dòng 34-45):

```java
public void connect(String host, int port) throws IOException {
    if (socket == null || socket.isClosed()) {
        this.socket = new Socket(host, port);
        // Quan trọng: Khởi tạo Output trước Input để tránh Deadlock
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.isRunning = true;

        // Chạy một luồng ngầm để liên tục nghe phản hồi từ Server
        startListening();
    }
}
```

**Phương thức `sendMessage()`** (dòng 48-55):

```java
public void sendMessage(NetworkMessage msg) {
    try {
        out.writeObject(msg);
        out.flush();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
```

**Phương thức `startListening()` - Listener Thread** (dòng 58-74):

```java
private void startListening() {
    Thread listenerThread = new Thread(() -> {
        try {
            while (isRunning) {
                // Đợi nhận phản hồi
                NetworkMessage response = (NetworkMessage) in.readObject();
                handleServerResponse(response);
            }
        } catch (Exception e) {
            log.warn("Lost connection to server.");
            isRunning = false;
        }
    });
    listenerThread.setDaemon(true); // Tự tắt khi ứng dụng đóng
    listenerThread.start();
}
```

**Đặc điểm listener thread**:
- **Daemon thread**: JVM có thể exit ngay cả khi thread đang chạy
- **Blocking read**: `in.readObject()` block cho đến khi có message
- **Exception handling**: Khi mất kết nối, set `isRunning = false`

**Observer Pattern - Listeners** (dòng 78-91):

```java
public void addListener(AuctionUpdateListener listener) {
    listeners.add(listener);
}

public void removeListener(AuctionUpdateListener listener) {
    listeners.remove(listener);
}

private void handleServerResponse(NetworkMessage response) {
    for (AuctionUpdateListener listener : listeners) {
        listener.onUpdateReceived(response);
    }
}
```

### 4.4 AuctionUpdateListener Interface

```java
package auction_client.interfaces;

import auction_shared.Network.NetworkMessage;

public interface AuctionUpdateListener {
    void onUpdateReceived(NetworkMessage msg);
}
```

**Observer Pattern**:
- ClientService là Subject (Observable)
- Các Controller đăng ký là Observers
- Khi có message từ server, thông báo tất cả observers

### 4.5 UserSession - Quản Lý Trạng Thái User

```java
package auction_client;

public class UserSession {
    private static UserSession self = null;
    private UserDTO user;
    private String username = "";

    private UserSession(){}

    public synchronized static UserSession getInstance(){
        if (self == null){
            self = new UserSession();
        }
        return self;
    }

    public void closeApp(){
        ClientService.getInstance().sendMessage(new NetworkMessage("LOGOUT", null));
        self = null;
    }
}
```

**Singleton Pattern**: Đảm bảo chỉ có một instance quản lý session trong ứng dụng

**Lưu trữ**:
- `user`: Thông tin user (id, username, role) dạng DTO
- `username`: Username string (thuận tiện cho việc hiển thị)

### 4.6 SignInController - Xử Lý Đăng Nhập

```java
public class SignInController implements Initializable, AuctionUpdateListener {
    @FXML public TextField username;
    @FXML public PasswordField password;
    public static final BooleanProperty isAdmin = new SimpleBooleanProperty(false);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);  // Đăng ký listener
    }

    @FXML
    public void onSignInClicked() {
        String inputUsername = username.getText().trim();
        String inputPassword = password.getText();

        if (inputUsername.isEmpty() || inputPassword.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ tài khoản và mật khẩu!", Alert.AlertType.WARNING);
            return;
        }

        SignUpDTO loginData = new SignUpDTO(null, inputUsername, inputPassword);
        ClientService.getInstance().sendMessage(new NetworkMessage("LOGIN", loginData));
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        if ("LOGIN".equals(msg.getAction())) {
            UserDTO user = (UserDTO) msg.getData();
            Platform.runLater(() -> {
                if (user != null) {
                    UserSession.getInstance().setUsername(username.getText().trim());
                    UserSession.getInstance().setUser(user);
                    boolean roleIsAdmin = "ADMIN".equalsIgnoreCase(UserSession.getInstance().getUser().getRole());
                    SignInController.isAdmin.set(roleIsAdmin);
                    switchToMainScene();
                } else {
                    showAlert("Thất bại", "Tài khoản hoặc mật khẩu không chính xác!", Alert.AlertType.ERROR);
                }
            });
        }
    }
}
```

**Chi tiết luồng đăng nhập**:

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Đăng Nhập Flow                                │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. User nhập username/password                                       │
│     └─ Validate: không empty                                          │
│                                                                       │
│  2. Client gửi: NetworkMessage("LOGIN", SignUpDTO)                   │
│     └─ ClientService.sendMessage()                                    │
│                                                                       │
│  3. Server xử lý: MessageHandlerService.handleLogin()                │
│     └─ UserService.login() → validate                                 │
│     └─ Server gửi: NetworkMessage("LOGIN", UserDTO)                   │
│                                                                       │
│  4. Client nhận: SignInController.onUpdateReceived()                  │
│     └─ Platform.runLater() → cập nhật UI thread                       │
│     └─ Nếu user ≠ null:                                              │
│         - Lưu UserSession                                            │
│         - Set isAdmin property                                        │
│         - switchToMainScene()                                         │
│     └─ Nếu user == null:                                             │
│         - Show error alert                                            │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

**`Platform.runLater()`**:
- JavaFX yêu cầu UI updates phải chạy trên JavaFX Application Thread
- `Platform.runLater()` schedule Runnable lên UI thread
- Cần thiết vì listener chạy trên socket listener thread

### 4.7 WebMenuBarController - Điều Hướng Scene

```java
public class WebMenuBarController implements Initializable {
    @FXML public Label welcome;
    @FXML public MenuButton productsMenuButton;
    @FXML public Button userProductListButton;
    @FXML public Button adminControlPanelButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Binding hiển thị theo role
        adminControlPanelButton.visibleProperty().bind(SignInController.isAdmin);
        productsMenuButton.visibleProperty().bind(SignInController.isAdmin.not());
        userProductListButton.visibleProperty().bind(SignInController.isAdmin.not());

        setWelcomeUsername(UserSession.getInstance().getUsername());
    }

    @FXML
    public void switchToMainScene(MouseEvent event) throws IOException {
        switchScene(event, "/auction_client/AuctionMain.fxml");
    }

    @FXML
    public void switchToUserProductListScene(ActionEvent event) throws IOException {
        ClientService.getInstance().sendMessage(
            new NetworkMessage("GET_MY_LIST", UserSession.getInstance().getUsername()));
        switchScene(event, "/auction_client/SellProductScene.fxml");
    }

    @FXML
    public void logOut(MouseEvent event) throws IOException {
        Alert logOutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        logOutAlert.setTitle("Logout");
        logOutAlert.setContentText("Are you sure you want to logout?");

        if (logOutAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            UserSession.getInstance().closeApp();
            switchScene(event, "/auction_client/SignInScene.fxml");
        }
    }

    private void switchScene(javafx.event.Event event, String fxmlPath) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = fxmlLoader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.getScene().setRoot(root);
        stage.centerOnScreen();
        stage.show();
    }
}
```

**Scene Navigation**:
- Dùng `FXMLLoader.load()` để load FXML file
- `Stage.getScene().setRoot()` thay thế root node
- Binding property cho hiển thị theo role (ADMIN vs USER)

### 4.8 Client FXML Scenes

#### SignInScene.fxml - Màn Hình Đăng Nhập
- TextField cho username
- PasswordField cho password
- Button "Sign In" gọi `onSignInClicked()`
- Hyperlink "Sign Up" chuyển sang `SignUpScene.fxml`

#### AuctionMain.fxml - Màn Hình Chính
- MenuBar với WebMenuBarController
- Container hiển thị nội dung theo menu

#### BidProductScene.fxml - Trang Xem Sản Phẩm
- Hiển thị danh sách auction
- Mỗi sản phẩm có thông tin: tên, giá hiện tại, thời gian còn lại, nút Bid

#### SellProductScene.fxml - Trang Quản Lý Sản Phẩm Đã Đăng
- Danh sách sản phẩm của user
- Thông tin bid history

#### ArtScene.fxml, ElectronicScene.fxml, VehicleScene.fxml
- Lọc sản phẩm theo ItemType

#### ActivitiesScene.fxml - Thông Báo
- Hiển thị danh sách Notification

#### AdminControlPanel.fxml - Trang Admin
- Quản lý users (ban/unban)
- Xem reports

---

## 5. Module Shared (auction-shared)

### 5.1 NetworkMessage

```java
package auction_shared.Network;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private String action;          // Action type (LOGIN, BID, BUY_OUT, ...)
    private Serializable data;     // Payload (DTO subclass)

    public NetworkMessage(String action, Serializable data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() { return this.action; }
    public Serializable getData() { return data; }
}
```

**Serializable**: Cần thiết để gửi qua ObjectInput/OutputStream

**Hai thành phần chính**:
- `action`: String xác định loại message (để router xử lý)
- `data`: Payload - object chứa dữ liệu

### 5.2 Notification

```java
package auction_shared.Network;

import java.io.Serializable;
import java.time.LocalTime;

public class Notification implements Serializable {
    private String notificationMSG;    // Nội dung thông báo
    private LocalTime notificationTime; // Thời gian tạo

    public Notification(String notificationMSG, LocalTime notificationTime) {
        this.notificationMSG = notificationMSG;
        this.notificationTime = notificationTime;
    }
}
```

**Mục đích**: Gửi thông báo hoạt động của user (VD: "you have placed bid successfully")

### 5.3 AuctionDTO

```java
package auction_shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionDTO implements Serializable {
    private String auctionId;
    private AuctionStatus status;
    private ItemDTO item;
    private ItemType type;
    private double startingPrice;
    private double buyOutPrice;
    private double tickSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean antiSniping;
    private String winnerId;
    private double currentHighestBid;

    public AuctionDTO(ItemDTO item, ItemType type, AuctionStatus status,
                     double startingPrice, double buyOutPrice, double tickSize,
                     LocalDateTime startTime, LocalDateTime endTime,
                     boolean antiSniping, String winnerId, double currentHighestBid) {
        // ... gán các giá trị
    }
}
```

### 5.4 ItemDTO

```java
package auction_shared.dto;

import java.io.Serializable;

public class ItemDTO implements Serializable {
    private String id;
    private String itemName;
    private String description;
    private UserDTO owner;
    private ItemType type;
}
```

### 5.5 BidTransactionDTO

```java
package auction_shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransactionDTO implements Serializable {
    private AuctionDTO auction;
    private UserDTO bidder;
    private double bidAmount;
    private LocalDateTime bidTime;
}
```

### 5.6 UserDTO

```java
package auction_shared.dto;

import java.io.Serializable;

public class UserDTO implements Serializable {
    private String id;
    private String username;
    private String role;  // "USER" hoặc "ADMIN"
}
```

**Lưu ý**: Không có password trong DTO - bảo mật

### 5.7 SignUpDTO

```java
package auction_shared.dto;

import java.io.Serializable;

public class SignUpDTO implements Serializable {
    private String id;
    private String username;
    private String password;
}
```

**Sử dụng cho cả Login và Register** (thiết kế có thể tách riêng)

### 5.8 Enums

#### AuctionStatus

```java
package auction_shared.dto;

public enum AuctionStatus {
    ACTIVE,   // Đang diễn ra
    ENDED,    // Kết thúc tự nhiên (hết giờ)
    SOLD      // Bán ngay (buy-out)
}
```

#### ItemType

```java
package auction_shared.dto;

public enum ItemType {
    ARTS,         // Đồ nghệ thuật
    ELECTRONICS,  // Đồ điện tử
    VEHICLES;     // Phương tiện

    public static ItemType fromDbValue(String value) {
        return switch (value) {
            case "ARTS" -> ARTS;
            case "ELECTRONICS" -> ELECTRONICS;
            case "VEHICLES" -> VEHICLES;
            default -> throw new IllegalArgumentException("Unknown item_type: " + value);
        };
    }

    public String toDbValue() {
        return this.name();
    }
}
```

---

## 6. Các Design Patterns Sử Dụng

### 6.1 Singleton Pattern

**Vị trí**: Nhiều nơi trong codebase

#### AuctionManager.java

```java
12:     private static volatile AuctionManager manager = null;

18:     public static synchronized AuctionManager getInstance() {
19:         if (manager != null) return manager;
20:         manager = new AuctionManager();
21:         return manager;
22:     }
```

**Mục đích**: Đảm bảo chỉ có một AuctionManager quản lý tất cả rooms và clients

**Thread-safety**:
- `volatile` đảm bảo visibility across threads
- `synchronized` đảm bảo only one thread tạo instance

#### ClientService.java

```java
18:     private static ClientService instance;

27:     public static synchronized ClientService getInstance(){
28:         if (instance != null) return instance;
29:         instance = new ClientService();
30:         return instance;
31:     }
```

**Mục đích**: Chỉ một kết nối socket cho toàn bộ ứng dụng client

#### UserSession.java

```java
8:     private static UserSession self = null;

13:     public synchronized static UserSession getInstance(){
14:         if (self == null) self = new UserSession();
15:         return self;
16:     }
```

**Mục đích**: Lưu trữ thông tin user đang đăng nhập trong suốt session

#### DatabaseConnection.java

```java
16:     private static HikariDataSource dataSource = null;
18:     private DatabaseConnection() {}  // Private constructor - không thể khởi tạo

20:     private static synchronized void init() {
21:         if (dataSource != null) return;
22:         // ... khởi tạo pool
23:     }

38:     public static Connection getConnection() throws SQLException {
39:         if (dataSource == null) init();
40:         return dataSource.getConnection();
41:     }
```

**Mục đích**: Chỉ một connection pool cho toàn bộ ứng dụng

### 6.2 Factory Pattern

**Vị trí**: `auction_server/factory/`

#### ItemFactory (Abstract Factory)

```java
7: public abstract class ItemFactory {
9:     public abstract Item create(String id, String name, String description, User owner);

11:     public static ItemFactory of(ItemType type) {
12:         return switch (type) {
13:             case ARTS -> new ArtsFactory();
14:             case ELECTRONICS -> new ElectronicsFactory();
15:             case VEHICLES -> new VehiclesFactory();
16:         };
17:     }
18: }
```

**Concrete Factories**:

```java
public class ArtsFactory extends ItemFactory {
    @Override
    public Item create(String id, String name, String description, User owner) {
        return new Arts(id, name, description, owner);
    }
}

public class ElectronicsFactory extends ItemFactory {
    @Override
    public Item create(String id, String name, String description, User owner) {
        return new Electronics(id, name, description, owner);
    }
}

public class VehiclesFactory extends ItemFactory {
    @Override
    public Item create(String id, String name, String description, User owner) {
        return new Vehicles(id, name, description, owner);
    }
}
```

**Sử dụng trong MessageHandlerService**:

```java
Item item = ItemFactory.of(itemDTO.getType()).create(
    itemDTO.getId(), itemDTO.getName(), itemDTO.getDescription(), owner);
```

**Mục đích**:
- Tạo Item concrete class dựa trên ItemType
- Tránh switch/if-else khi tạo object
- Dễ thêm loại item mới (chỉ cần thêm Factory + subclass)

### 6.3 Observer Pattern

**Vị trí**: Client side - ClientService + AuctionUpdateListener

#### Subject (Observable)

```java
public class ClientService {
    private final List<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(AuctionUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AuctionUpdateListener listener) {
        listeners.remove(listener);
    }

    private void handleServerResponse(NetworkMessage response) {
        for (AuctionUpdateListener listener : listeners) {
            listener.onUpdateReceived(response);
        }
    }
}
```

#### Observer Interface

```java
public interface AuctionUpdateListener {
    void onUpdateReceived(NetworkMessage msg);
}
```

#### Concrete Observers

```java
public class SignInController implements Initializable, AuctionUpdateListener {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ClientService.getInstance().addListener(this);
    }

    @Override
    public void onUpdateReceived(NetworkMessage msg) {
        if ("LOGIN".equals(msg.getAction())) {
            // Xử lý login response
        }
    }
}
```

**Mục đích**:
- ClientService thông báo cho tất cả controllers khi có message từ server
- Mỗi controller xử lý các message mà nó quan tâm
- Decouple giữa network layer và UI controllers

### 6.4 Strategy Pattern (Behavior Interfaces)

**Vị trí**: `auction_server/behaviors/`

#### Interfaces

```java
public interface BidderProfile {
    void placeBid(String itemId, double amount);
}

public interface SellerProfile {
    void postItem(Item item);
}

public interface AdminProfile {
    void manageUsers();
    void viewReports();
    void banUser(String userId);
}
```

#### Concrete Implementations

```java
public class BidderBehaviors implements BidderProfile {
    private final User currentUser;

    @Override
    public void placeBid(String itemId, double amount) {
        Auction auction = AuctionManager.getInstance().getRoom(itemId);
        if (auction == null) return;
        BidTransaction transaction = new BidTransaction(auction, currentUser, amount);
        auction.placeBid(transaction);
    }
}

public class SellerBehaviors implements SellerProfile {
    private static final ItemDAO itemDAO = new ItemDAO();

    @Override
    public void postItem(Item item) {
        itemDAO.insert(item);
    }
}
```

#### Usage in User Entity

```java
public class User extends Entity {
    private BidderProfile bidder = null;
    private SellerProfile seller = null;
    private AdminProfile adminProfile = null;

    private void initBehaviors() {
        if ("ADMIN".equals(role)) {
            this.adminProfile = new AdminBehaviors();
        } else if ("USER".equals(role)) {
            this.bidder = new BidderBehaviors(this);
            this.seller = new SellerBehaviors();
        }
    }

    public void performBid(String itemId, double amount) {
        if (bidder != null) bidder.placeBid(itemId, amount);
    }

    public void performPost(Item item) {
        if (seller != null) seller.postItem(item);
    }
}
```

**Mục đích**:
- User có các "hành vi" khác nhau dựa trên role
- ADMIN có AdminProfile, USER có BidderProfile + SellerProfile
- Dễ mở rộng thêm behavior mới

### 6.5 Dependency Injection / Inversion of Control

**Vị trí**: MessageHandlerService

```java
public class MessageHandlerService {
    private final MessageSender messageSender;
    private final LogoutHandler logoutHandler;

    public interface MessageSender {
        void sendMessage(NetworkMessage msg);
    }

    public interface LogoutHandler {
        void onLogout();
    }

    public MessageHandlerService(List<Notification> activities,
                               MessageSender messageSender,
                               LogoutHandler logoutHandler,
                               DAOProvider daoProvider) {
        this.messageSender = messageSender;
        this.logoutHandler = logoutHandler;
        // ...
    }
}
```

**Sử dụng trong ClientHandler**:

```java
this.messageHandler = new MessageHandlerService(
    activities,
    this::sendMessage,    // Method reference
    this::onLogout,      // Method reference
    daoProvider
);
```

**Mục đích**:
- MessageHandlerService không tạo dependencies trực tiếp
- Dependencies được inject từ bên ngoài (ClientHandler)
- Dễ test (mock dependencies)
- Loose coupling

### 6.6 DAO Pattern (Data Access Object)

**Vị trí**: `auction_server/dao/`

#### DAO Interfaces

```java
public interface WritableDAO<T> {
    int insert(T t);
    int delete(T t);
    int update(T t);
}

public interface ReadableDAO<T> {
    ArrayList<T> selectAll();
    T selectById(T t);
    ArrayList<T> selectByCondition(String condition);
}

public interface TransactionalDAO<T> {
    int insert(T t, Connection conn) throws SQLException;
}
```

#### Concrete DAOs

```java
public class AuctionDAO implements WritableDAO<Auction> {
    @Override
    public int insert(Auction auction) { /* ... */ }
    @Override
    public int delete(Auction auction) { return 0; }
    @Override
    public int update(Auction auction) { /* ... */ }
}

public class BidTransactionDAO implements TransactionalDAO<BidTransaction> {
    @Override
    public int insert(BidTransaction bt, Connection conn) throws SQLException { /* ... */ }
}
```

**Mục đích**:
- Tách biệt business logic và data access
- Các DAO implementations chịu trách nhiệm tương tác SQL
- Interface định nghĩa contract

### 6.7 Mapper Pattern

**Vị trí**: `auction_server/mapper/Mappers.java`

```java
public class Mappers {
    public static UserDTO toDTO(User user) {
        if (user == null) return null;
        return new UserDTO(user.getId(), user.getUsername(), user.getRole());
    }

    public static ItemDTO toDTO(Item item) {
        if (item == null) return null;
        return new ItemDTO(item.getId(), item.getName(), item.getDescription(),
                          toDTO(item.getOwner()), item.getType());
    }

    public static AuctionDTO toDTO(Auction auction) {
        if (auction == null) return null;
        return new AuctionDTO(toDTO(auction.getItem()), /* ... */);
    }
}
```

**Mục đích**:
- Chuyển đổi giữa Entity (server) và DTO (shared)
- Tách biệt internal model và external representation
- Không gửi password, internal fields ra client

### 6.8 Template Method Pattern

**Vị trí**: BidService.processAndSaveBid()

```java
public boolean processAndSaveBid(Auction auction, BidTransaction transaction) {
    if (!auction.placeBid(transaction)) return false;  // Template step 1

    try (Connection conn = DatabaseConnection.getConnection()) {
        conn.setAutoCommit(false);
        try {
            bidDAO.insert(transaction, conn);            // Template step 2
            auctionDAO.updateHighestBid(auction, conn); // Template step 3
            conn.commit();                               // Template step 4
            // Anti-sniping logic...
            return true;
        } catch (SQLException e) {
            conn.rollback();                             // Template: rollback on failure
            auction.revertLastBid(transaction);          // Template: revert state
            return false;
        }
    } catch (SQLException e) {
        auction.revertLastBid(transaction);
        return false;
    }
}
```

**Mục đích**:
- Định nghĩa skeleton của thuật toán (validate → insert → update → commit)
- Các bước cụ thể được implement trong DAO
- Error handling và rollback logic tập trung

### 6.9 Memento Pattern (Rollback)

**Vị trí**: Auction.revertLastBid() và Auction.revertBuyOut()

```java
public void revertLastBid(BidTransaction transaction) {
    lock.lock();
    try {
        if (!bidHistory.isEmpty() && bidHistory.get(bidHistory.size() - 1).equals(transaction)) {
            bidHistory.remove(bidHistory.size() - 1);
            if (bidHistory.isEmpty()) {
                this.currentHighestBid = this.startingPrice;
            } else {
                this.currentHighestBid = bidHistory.get(bidHistory.size() - 1).getBidAmount();
            }
        }
    } finally { lock.unlock(); }
}

public void revertBuyOut(BidTransaction transaction) {
    lock.lock();
    try {
        if (originalOwnerBeforeBuyOut != null) {
            item.setOwner(originalOwnerBeforeBuyOut);
            originalOwnerBeforeBuyOut = null;
        }
        status = AuctionStatus.ACTIVE;
        winnerId = null;
    } finally { lock.unlock(); }
}
```

**Mục đích**:
- Khôi phục trạng thái in-memory khi DB transaction thất bại
- `originalOwnerBeforeBuyOut` lưu trạng thái trước đó (memento)

---

## 7. Chi Tiết Từng Method và Luồng Hoạt Động

### 7.1 End-to-End Flow: Đặt Giá Thầu (Place Bid)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    END-TO-END: PLACE BID FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  CLIENT SIDE                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 1. User nhấn nút "Bid" trên sản phẩm                                 │    │
│  │ 2. Controller tạo BidTransactionDTO:                                 │    │
│  │    new BidTransactionDTO(auctionDTO, bidderDTO, bidAmount)           │    │
│  │ 3. ClientService.sendMessage(                                         │    │
│  │       new NetworkMessage("PLACE_BID", bidTransactionDTO))             │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               │ TCP Socket                                     │
│                               ▼                                               │
│  SERVER SIDE                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 4. ClientHandler nhận NetworkMessage                                 │    │
│  │ 5. handleRequest() route đến handlePlaceBid()                        │    │
│  │ 6. MessageHandlerService.handlePlaceBid():                           │    │
│  │    - Parse BidTransactionDTO                                         │    │
│  │    - Lấy Auction từ AuctionManager.getRoom(itemId)                   │    │
│  │    - Tạo BidTransaction entity                                       │    │
│  │    - Gọi BidService.processAndSaveBid(auction, transaction)          │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               │                                               │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 7. BidService.processAndSaveBid():                                   │    │
│  │    a. auction.placeBid(transaction):                                 │    │
│  │       - ReentrantLock.lock()                                         │    │
│  │       - Validate: status, expiration, owner, amount, tickSize        │    │
│  │       - bidHistory.add(transaction)                                  │    │
│  │       - currentHighestBid = bidAmount                                 │    │
│  │       - ReentrantLock.unlock()                                       │    │
│  │       - Return true/false                                            │    │
│  │    b. Nếu placeBid() = true:                                         │    │
│  │       - conn = DatabaseConnection.getConnection()                    │    │
│  │       - conn.setAutoCommit(false)                                    │    │
│  │       - bidDAO.insert(transaction, conn)  → INSERT bid_transactions  │    │
│  │       - auctionDAO.updateHighestBid(auction, conn)                   │    │
│  │       - conn.commit()                                                │    │
│  │       - Nếu antiSniping: extendTime() + updateEndTime                │    │
│  │       - Return true                                                  │    │
│  │    c. Nếu có lỗi SQLException:                                       │    │
│  │       - conn.rollback()                                              │    │
│  │       - auction.revertLastBid(transaction)  ← RAM rollback          │    │
│  │       - Return false                                                 │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               │                                               │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 8. MessageHandler nhận kết quả:                                     │    │
│  │    - Nếu success:                                                   │    │
│  │      * Gửi NetworkMessage("BID_SUCCESS", auctionDTO) cho bidder     │    │
│  │      * Broadcast NetworkMessage("UPDATE_BID", list) cho tất cả       │    │
│  │      * Log activity                                                  │    │
│  │    - Nếu fail:                                                      │    │
│  │      * Gửi NetworkMessage("BID_FAILED", null) cho bidder            │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               │ TCP Socket                                     │
│                               ▼                                               │
│  CLIENT SIDE                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 9. ClientService.handleServerResponse() thông báo listeners         │    │
│  │ 10. Controller.onUpdateReceived() nhận message:                      │    │
│  │     - Nếu "BID_SUCCESS": hiển thị thông báo thành công              │    │
│  │     - Nếu "UPDATE_BID": cập nhật danh sách sản phẩm                 │    │
│  │     - Nếu "BID_FAILED": hiển thị thông báo lỗi                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 End-to-End Flow: Mua Ngay (Buy Out)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    END-TO-END: BUY OUT FLOW                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  CLIENT SIDE                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 1. User nhấn nút "Buy Out" trên sản phẩm                            │    │
│  │ 2. Controller tạo BidTransactionDTO với bidAmount = buyOutPrice    │    │
│  │ 3. ClientService.sendMessage(                                        │    │
│  │       new NetworkMessage("BUY_OUT", bidTransactionDTO))              │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  SERVER SIDE                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 4. MessageHandler.handleBuyOut():                                   │    │
│  │    - Parse BidTransactionDTO                                         │    │
│  │    - Lấy Auction từ AuctionManager                                  │    │
│  │    - Gọi BidService.processBuyOut(auction, transaction)              │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 5. BidService.processBuyOut():                                       │    │
│  │    a. auction.buyOut(transaction):                                   │    │
│  │       - Lock                                                          │    │
│  │       - Validate: status, expiration, owner, amount                  │    │
│  │       - originalOwnerBeforeBuyOut = item.owner                        │    │
│  │       - item.setOwner(bidder)  ← Chuyển quyền sở hữu                 │    │
│  │       - status = SOLD                                                 │    │
│  │       - winnerId = bidder.id                                          │    │
│  │       - Unlock                                                        │    │
│  │    b. Nếu buyOut() = true:                                           │    │
│  │       - bidDAO.insert(transaction, conn)                              │    │
│  │       - auctionDAO.updateStatusAndWinner(auction, conn)               │    │
│  │       - conn.commit()                                                 │    │
│  │    c. Nếu lỗi: rollback + revertBuyOut()                             │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 6. MessageHandler xử lý kết quả:                                     │    │
│  │    - AuctionManager.removeRoom(auction)  ← Xóa khỏi active rooms     │    │
│  │    - Tìm winner client:                                              │    │
│  │      for (ClientHandler client : activeClients)                      │    │
│  │        if (client.getLoggedInUser().getId() == winnerId)             │    │
│  │          client.sendMessage("YOU_WON", auctionDTO)                   │    │
│  │    - Broadcast "AUCTION_ENDED" + "UPDATE_BID"                       │    │
│  │    - Gửi "BUYOUT_SUCCESS" cho người mua                             │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                               ▼                                               │
│  CLIENT SIDE (Winner receives YOU_WON)                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 7. Controller hiển thị thông báo "YOU_WON!"                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.3 End-to-End Flow: AuctionScheduler Xử Lý Auction Hết Hạn

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    END-TO-END: AUCTION EXPIRATION FLOW                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  EVERY 1 SECOND (Background Thread)                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ AuctionScheduler.checkExpiredAuctions():                            │    │
│  │    for (Auction auction : auctionManager.getAllRooms()) {            │    │
│  │      if (auction.isExpired()) {  // now.isAfter(endTime)            │    │
│  │                                                                         │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ 1. auction.endAuction():                                     │   │    │
│  │  │    - Lock                                                    │   │    │
│  │  │    - status = ENDED                                          │   │    │
│  │  │    - winnerId = highestBidder.id                             │   │    │
│  │  │    - Unlock                                                  │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                         │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ 2. WinnerService.determineWinner(bidHistory):              │   │    │
│  │  │    - Duyệt từ cuối list (bid cao nhất)                      │   │    │
│  │  │    - Kiểm tra user không bị BANNED                          │   │    │
│  │  │    - Return winnerId hoặc null                              │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                         │    │
│  │  3. auctionDAO.update(auction)  → UPDATE DB: status, winnerId       │    │
│  │  4. auctionManager.removeRoom(auction)  ← Xóa khỏi RAM             │    │
│  │                                                                         │    │
│  │  ┌─────────────────────────────────────────────────────────────┐   │    │
│  │  │ 5. Notify Winner (if exists):                               │   │    │
│  │  │    for (ClientHandler client : activeClients) {              │   │    │
│  │  │      if (client.getLoggedInUser().getId() == winnerId)      │   │    │
│  │  │        client.sendMessage("YOU_WON", auctionDTO)            │   │    │
│  │  │    }                                                         │   │    │
│  │  └─────────────────────────────────────────────────────────────┘   │    │
│  │                                                                         │    │
│  │  6. Broadcast to ALL clients:                                       │    │
│  │     - "AUCTION_ENDED" + auctionDTO                                  │    │
│  │     - "UPDATE_BID" + danhSáchAuctionMới                             │    │
│  │                                                                         │    │
│  │      }  // end if isExpired                                          │    │
│  │    }  // end for                                                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Cơ Chế Anti-Sniping

### 8.1 Vấn Đề Sniping

**Sniping** là hành vi đặt giá vào phút cuối cùng của auction để giành chiến thắng mà không cho người khác cơ hội phản đáp.

**Ví dụ**: Auction kết thúc lúc 10:00:00. Snipers đặt giá lúc 9:59:59, giành chiến thắng trước khi ai kịp phản ứng.

### 8.2 Giải Pháp Của Hệ Thống

Hệ thống sử dụng **Time Extension** (gia hạn thời gian) kết hợp **Grace Period**.

### 8.3 Chi Tiết Cài Đặt

```java
// Auction.java
private static final long SNIPING_GRACE_SECONDS = 30;  // 30 giây cuối
private static final long EXTENSION_SECONDS = 30;       // Thêm 30 giây
```

### 8.4 Luồng Hoạt Động

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ANTI-SNIPING MECHANISM                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  SCENARIO: Auction có antiSniping = true, endTime = 10:00:00                 │
│                                                                              │
│  TIME: 09:59:28  (32 giây trước khi hết hạn)                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Bid placement at 09:59:28                                          │    │
│  │ - remaining = 32 seconds                                           │    │
│  │ - 32 > 30 (GRACE_PERIOD)?  → YES                                   │    │
│  │ - NO extension applied                                             │    │
│  │ - endTime = 10:00:00 (unchanged)                                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  TIME: 09:59:35  (25 giâu trước khi hết hạn)                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Bid placement at 09:59:35                                          │    │
│  │ - remaining = 25 seconds                                           │    │
│  │ - 25 <= 30 (GRACE_PERIOD) AND 25 > 0?  → YES                      │    │
│  │ - END TIME EXTENDED!                                               │    │
│  │ - endTime = 10:00:00 + 30 seconds = 10:00:30                      │    │
│  │ - Database updated with new endTime                                │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  TIME: 09:59:50  (40 giây trước khi hết hạn MỚI = 10:00:30)                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Bid placement at 09:59:50                                          │    │
│  │ - remaining = 40 seconds                                           │    │
│  │ - 40 > 30?  → YES                                                  │    │
│  │ - NO extension (still in extended period but not in grace)          │    │
│  │ - endTime = 10:00:30 (unchanged)                                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  TIME: 10:00:15  (15 giâu trước khi hết hạn)                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Bid placement at 10:00:15                                          │    │
│  │ - remaining = 15 seconds                                           │    │
│  │ - 15 <= 30 AND 15 > 0?  → YES                                      │    │
│  │ - END TIME EXTENDED AGAIN!                                         │    │
│  │ - endTime = 10:00:30 + 30 seconds = 10:01:00                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  RESULT:                                                                    │
│  - Snipers cannot win at the last second            │
│  - Each late bid extends the auction by 30 seconds │
│  - Fair competition maintained                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.5 Code Implementation

**Trong Auction.java - extendTime()**:

```java
public void extendTime() {
    lock.lock();
    try {
        if (status != AuctionStatus.ACTIVE) return;

        long remaining = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        // Chỉ extend nếu còn <= 30 giây VÀ > 0 giây
        if (remaining <= SNIPING_GRACE_SECONDS && remaining > 0) {
            endTime = endTime.plusSeconds(EXTENSION_SECONDS);
        }
    } finally {
        lock.unlock();
    }
}
```

**Trong BidService.processAndSaveBid()**:

```java
if (auction.isAntiSniping()) {
    LocalDateTime oldEndTime = auction.getEndTime();
    auction.extendTime();
    if (!oldEndTime.equals(auction.getEndTime())) {
        // Chỉ update DB nếu endTime thực sự thay đổi
        auctionDAO.updateEndTime(auction, conn);
    }
}
```

### 8.6 Anti-Sniping KHÔNG Áp Dụng Cho Buy-Out

**Lý do**: Buy-out là hành động chủ động "mua ngay" với giá cố định, không phải bidding competition. Không có sniping scenario.

**Code check trong Auction.buyOut()**:

```java
public boolean buyOut(BidTransaction transaction) {
    // Không có anti-sniping logic ở đây
    // Buy-out kết thúc auction ngay lập tức
    status = AuctionStatus.SOLD;
    // ...
}
```

---

## 9. Cơ Chế Thread-Safety

### 9.1 Tổng Quan

Hệ thống là multi-threaded với:
- **Server**: Nhiều ClientHandlers (Virtual Threads) + AuctionScheduler (Single Thread)
- **Client**: Socket listener thread + JavaFX Application thread

### 9.2 Thread-Safe Data Structures

```java
// AuctionManager.java
private final Map<String, Auction> activeRooms = new ConcurrentHashMap<>();
private final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
```

| Cấu trúc | Loại | Thread-Safety |
|----------|------|---------------|
| `ConcurrentHashMap` | Map | - Thread-safe read/write<br>- Không throw ConcurrentModificationException<br>- Phù hợp cho read-heavy workload |
| `CopyOnWriteArrayList` | List | - Thread-safe iteration<br>- Add/remove tạo bản copy mới<br>- Phù hợp cho write ít, read nhiều (broadcast) |

### 9.3 ReentrantLock Trong Auction Entity

```java
public class Auction implements Serializable {
    private final ReentrantLock lock = new ReentrantLock();

    public boolean placeBid(BidTransaction transaction) {
        lock.lock();
        try {
            // Critical section - thao tác với auction state
            if (status != AuctionStatus.ACTIVE) return false;
            // ... validation và update
            return true;
        } finally {
            lock.unlock();
        }
    }
}
```

**Tại sao cần Lock?**
- Nhiều ClientHandlers có thể gọi `placeBid()` đồng thời
- Scheduler kiểm tra `isExpired()` và `endAuction()`
- Cần đảm bảo atomic operations

**Tại sao là ReentrantLock?**
- Reentrant: cùng thread có thể acquire lock nhiều lần
- Công bằng hơn `synchronized`
- Có thể try-lock (non-blocking)

### 9.4 Synchronized Trong ClientHandler

```java
public synchronized void sendMessage(NetworkMessage msg) {
    try {
        out.writeObject(msg);
        out.flush();
        out.reset();
    } catch (IOException e) {
        log.info("fail to send message", e);
    }
}
```

**Tại sao synchronized?**
- Nhiều threads có thể gọi `sendMessage()`:
  - ClientHandler's run() thread
  - AuctionScheduler's thread (broadcast)
- `out.writeObject()` không thread-safe
- Dùng synchronized thay vì ReentrantLock vì đơn giản và đủ hiệu quả

### 9.5 CopyOnWriteArrayList Trong ClientService

```java
public class ClientService {
    private final List<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();
}
```

**Sử dụng khi nào?**
- Nhiều threads đọc (listener thread thông báo tất cả)
- Ít threads ghi (add/remove listener hiếm khi)
- Iterator không throw ConcurrentModificationException

### 9.6 Race Conditions và Giải Pháp

#### Race Condition 1: Bidding Cùng Lúc

```
THREAD A                          THREAD B
────────                          ────────
placeBid(amount=100)
  ├─ lock.acquire()
  ├─ currentHighestBid = 50
  ├─ 100 > 50 → OK
  ├─ bidHistory.add(100)
  ├─ setCurrentHighestBid(100)
  ├─ lock.release()
  │                                 placeBid(amount=75)
  │                                   ├─ lock.acquire()
  │                                   ├─ currentHighestBid = 100
  │                                   ├─ 75 > 100 → REJECT
  │                                   └─ lock.release()
```

**Giải pháp**: Lock trong placeBid() đảm bảo serialization

#### Race Condition 2: Bid vs Scheduler Check

```
THREAD A (Bid)                    THREAD B (Scheduler)
────────────                      ────────────────────
placeBid(amount=100)
  ├─ lock.acquire()
  ├─ validate OK
  ├─ bidHistory.add(100)
  │                                 isExpired() → FALSE
  │                                   (endTime chưa đến)
  │                                 check tiếp auction khác
  │                                 
  └─ lock.release()
```

**Giải pháp**: Lock trong cả placeBid() và endAuction()

#### Race Condition 3: Double-Checked Locking Singleton

```java
public static synchronized AuctionManager getInstance() {
    if (manager != null) return manager;  // Check 1
    manager = new AuctionManager();        // Check 2
    return manager;
}
```

**Giải pháp**: `synchronized` đảm bảo chỉ một thread tạo instance

### 9.7 Virtual Threads (Java 21)

```java
// SocketServer.java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

**Ưu điểm**:
- Nhẹ hơn OS threads truyền thống
- Có thể xử lý hàng nghìn concurrent connections
- Không cần thread pool phức tạp

**Nhược điểm**:
- Blocking I/O vẫn block virtual thread (nhưng đã dùng try-with-resources)
- Không dùng cho CPU-intensive tasks

---

## 10. Phục Hồi Database (Server Restart)

### 10.1 Vấn Đề

Khi server restart, tất cả in-memory state (AuctionManager, activeRooms, bidHistory) bị mất. Cần phục hồi từ database.

### 10.2 Quy Trình Phục Hồi

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SERVER RESTART RECOVERY FLOW                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Main.main() được gọi                                                        │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 1. DAOProvider daoProvider = new DefaultDAOProvider();              │    │
│  │    └─ Tạo các DAO instances                                          │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 2. AuctionDAO.selectActiveAuctions()                                 │    │
│  │    └─ SQL: SELECT a.*, i.*, u.*                                      │    │
│  │       FROM auctions a                                                │    │
│  │       JOIN items i ON a.item_id = i.id                              │    │
│  │       JOIN users u ON i.owner_id = u.id                             │    │
│  │       WHERE a.auction_status = 'ACTIVE'                              │    │
│  │    └─ Trả về List<Auction> với Item và Owner đã joined               │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 3. AuctionManager.getInstance().addRoom(auction)                     │    │
│  │    └─ Thêm auction vào ConcurrentHashMap                             │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 4. BidTransactionDAO.selectByAuctionId(auctionId)                    │    │
│  │    └─ SQL: SELECT bt.*, u.username                                   │    │
│  │       FROM bid_transactions bt                                       │    │
│  │       JOIN users u ON bt.bidder_id = u.id                            │    │
│  │       WHERE bt.auction_id = ?                                        │    │
│  │       ORDER BY bt.bid_time ASC                                       │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 5. auction.setBidHistory(history)                                   │    │
│  │    └─ Gán lại bidHistory từ database                                │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 6. AuctionScheduler.start()                                          │    │
│  │    └─ Bắt đầu kiểm tra expired auctions mỗi giây                      │    │
│  └────────────────────────────┬────────────────────────────────────────────┘    │
│                               ▼                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ 7. SocketServer.start(8080)                                         │    │
│  │    └─ Bắt đầu chấp nhận client connections                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  KẾT QUẢ:                                                                   │
│  - Tất cả ACTIVE auctions được load vào RAM                                 │
│  - Bid history được phục hồi cho mỗi auction                                 │
│  - Clients có thể tiếp tục bid trên các auctions                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 10.3 Các Bảng Database

**users**
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(20),        -- 'USER' hoặc 'ADMIN'
    user_status VARCHAR(20)   -- 'AVAILABLE' hoặc 'BANNED'
);
```

**items**
```sql
CREATE TABLE items (
    id VARCHAR(36) PRIMARY KEY,
    item_type VARCHAR(20),   -- 'ARTS', 'ELECTRONICS', 'VEHICLES'
    item_name VARCHAR(255),
    description TEXT,
    owner_id VARCHAR(36),
    FOREIGN KEY (owner_id) REFERENCES users(id)
);
```

**auctions**
```sql
CREATE TABLE auctions (
    id VARCHAR(36) PRIMARY KEY,  -- = item_id
    item_id VARCHAR(36),
    starting_price DECIMAL(10,2),
    buy_out_price DECIMAL(10,2),
    tick_size DECIMAL(10,2),
    current_highest_bid DECIMAL(10,2),
    start_time DATETIME,
    end_time DATETIME,
    auction_status VARCHAR(20),  -- 'ACTIVE', 'ENDED', 'SOLD'
    winner_id VARCHAR(36),
    anti_snipe BOOLEAN,
    FOREIGN KEY (item_id) REFERENCES items(id)
);
```

**bid_transactions**
```sql
CREATE TABLE bid_transactions (
    id VARCHAR(36) PRIMARY KEY,
    auction_id VARCHAR(36),
    bidder_id VARCHAR(36),
    bid_amount DECIMAL(10,2),
    bid_time DATETIME,
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);
```

### 10.4 Constructor Phục Hồi Trong Auction

```java
/**
 * Constructor dùng để rebuild Auction entity từ database khi server khởi động lại.
 * currentHighestBid và status được truyền trực tiếp thay vì tính toán lại.
 */
public Auction(Item item, double startingPrice, double buyOutPrice, double tickSize,
              LocalDateTime startTime, LocalDateTime endTime, boolean antiSniping,
              double currentHighestBid, AuctionStatus status) {
    this.auctionId = item.getId();
    this.item = item;
    this.startingPrice = startingPrice;
    this.buyOutPrice = buyOutPrice;
    this.tickSize = tickSize;
    this.startTime = startTime;
    this.endTime = endTime;
    this.antiSniping = antiSniping;
    this.currentHighestBid = currentHighestBid;  // Từ DB, không tính lại
    this.status = status;                        // Từ DB
    this.winnerId = null;                         // ACTIVE auction chưa có winner
}
```

### 10.5 Rebuild Item Từ Database

```java
private Item mapRowToItem(ResultSet rs, User owner) throws SQLException {
    String id = rs.getString("item_id");
    String name = rs.getString("item_name");
    String description = rs.getString("description");
    ItemType type = ItemType.fromDbValue(rs.getString("item_type"));

    return switch (type) {
        case ARTS -> new Arts(id, name, description, owner);
        case ELECTRONICS -> new Electronics(id, name, description, owner);
        case VEHICLES -> new Vehicles(id, name, description, owner);
    };
}
```

### 10.6 Rebuild Bid History

```java
public ArrayList<BidTransaction> selectByAuctionId(String auctionId) {
    String sql = "SELECT bt.*, u.username, u.user_status " +
                 "FROM bid_transactions bt " +
                 "JOIN users u ON bt.bidder_id = u.id " +
                 "WHERE bt.auction_id = ? ORDER BY bt.bid_time ASC";

    while (rs.next()) {
        User bidder = new User(rs.getString("bidder_id"), rs.getString("username"), null);
        BidTransaction tx = new BidTransaction(null, bidder, rs.getDouble("bid_amount"));
        tx.setId(rs.getString("id"));
        tx.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
        result.add(tx);
    }
}
```

### 10.7 Giới Hạn Của Recovery

**Đã Phục Hồi**:
- Auction details (prices, times, status)
- Bid history (từ bid_transactions)
- Item info và owner

**Chưa Phục Hồi**:
- `originalOwnerBeforeBuyOut` (transient field) - không cần thiết vì đã SOLD
- `activities` list của mỗi ClientHandler - reset khi client reconnect
- Client connections - clients phải reconnect

---

## Tổng Kết

### Các Design Patterns Sử Dụng

| Pattern | Vị trí | Mục đích |
|---------|--------|----------|
| Singleton | AuctionManager, ClientService, UserSession, DatabaseConnection | Đảm bảo một instance |
| Factory | ItemFactory, ArtsFactory, etc. | Tạo Item theo type |
| Observer | ClientService + AuctionUpdateListener | Thông báo khi có update |
| Strategy | BidderProfile, SellerProfile, AdminProfile | Behavior theo role |
| DAO | AuctionDAO, UserDAO, etc. | Tách biệt data access |
| Mapper | Mappers | Chuyển đổi Entity ↔ DTO |
| Dependency Injection | MessageHandlerService | Inject dependencies |
| Template Method | BidService.processAndSaveBid() | Skeleton algorithm |
| Memento | revertLastBid(), revertBuyOut() | Rollback state |

### Các Cơ Chế Quan Trọng

| Cơ chế | Mô tả |
|--------|-------|
| **Anti-Sniping** | Gia hạn 30s nếu có bid trong 30s cuối |
| **Thread-Safety** | ReentrantLock, ConcurrentHashMap, CopyOnWriteArrayList |
| **Transaction Safety** | DB rollback + RAM rollback khi lỗi |
| **Virtual Threads** | Java 21 - hỗ trợ hàng nghìn concurrent connections |
| **HikariCP** | Connection pool với max 10 connections |

### Tech Stack

| Layer | Công nghệ |
|-------|-----------|
| Server | Java 21, SLF4J |
| Client | JavaFX, FXML |
| Network | Java Socket, ObjectInputStream/ObjectOutputStream |
| Database | MySQL, HikariCP |
| Build | Maven |
