# Auction-System_Team5

<div align="center">

![Auction System Logo](./auction-client/src/main/resources/auctionclient/images/Auction-System.png)

**Hệ thống Đấu giá Trực tuyến Thời gian thực** — Xây dựng với Java 21, JavaFX & TCP/IP Socket

*Môn học: Lập trình nâng cao*

</div>

---

## Mục lục

- [Thông tin Học thuật](#thông-tin-học-thuật)
- [Giới thiệu Tổng quan](#giới-thiệu-tổng-quan)
- [Tính năng Chính](#tính-năng-chính)
- [Kiến trúc & Thiết kế OOP](#kiến-trúc--thiết-kế-oop)
- [Tech Stack](#tech-stack)
- [Cấu trúc Dự án](#cấu-trúc-dự-án)
- [Hướng dẫn Cài đặt & Chạy chương trình](#hướng-dẫn-cài-đặt--chạy-chương-trình)
- [Tài liệu & Demo](#tài-liệu--demo)

---

## Thông tin Học thuật

| | |
|---|---|
| **Môn học** | Lập trình nâng cao |
| **Nhóm** | Team 5 |
| **Thành viên** | Phan Trần Thiện Nhân · Phạm Phương Nam · Vũ Quang Minh · Nguyễn Vũ Quang Minh |

---

## Giới thiệu Tổng quan

Auction-System_Team5 là một hệ thống đấu giá trực tuyến thời gian thực, phát triển theo mô hình **client-server** với giao tiếp qua **TCP/IP Socket**. Hệ thống cho phép người dùng đăng nhập với vai trò **Người đấu giá (Bidder)**, **Người bán (Seller)** hoặc **Quản trị viên (Admin)**, tham gia đấu giá sản phẩm theo thời gian thực, đặt giá thầu (place bid) hoặc mua ngay (buy-out).

**Điểm nhấn kỹ thuật cốt lõi:**

- **Real-time bidding** — Cập nhật giá thầu tức thì qua Socket broadcasting tới tất cả client đang kết nối.
- **Multi-module Maven** — Tách biệt logic chia sẻ (`auction-shared`) giữa server và client, đảm bảo tính nhất quán DTO.
- **PostgreSQL (AWS RDS)** với **HikariCP connection pool** — Quản lý kết nối database hiệu quả, thread-safe.
- **Virtual Threads (Java 21)** — Server xử lý hàng trăm kết nối đồng thời bằng `Executors.newVirtualThreadPerTaskExecutor()`.
- **Transaction & Rollback** — Toàn bộ luồng xử lý bid/buyout được bảo vệ bởi `ReentrantLock` + JDBC Transaction, đảm bảo tính nhất quán dữ liệu ngay cả khi DB gặp sự cố giữa chừng.
- **Server Restart Recovery** — Khi server khởi động lại, toàn bộ phiên đấu giá đang ACTIVE và lịch sử trả giá được tải lại từ DB vào RAM để tiếp tục hoạt động không gián đoạn.

---

## Tính năng Chính

### Người dùng (Bidder / Seller)

| Tính năng | Mô tả |
|---|---|
| **Đăng nhập / Đăng ký** | Xác thực người dùng qua Socket; phân quyền tự động theo vai trò (ADMIN / USER) |
| **Duyệt sản phẩm đang đấu giá** | Xem danh sách phiên ACTIVE theo thời gian thực |
| **Xem sản phẩm đã kết thúc** | Tra cứu lịch sử các phiên đã ENDED/SOLD (lấy từ DB) |
| **Đặt giá thầu (Place Bid)** | Ra giá theo bước (tick size); xác thực chống tự mua, sai bước giá, hết giờ |
| **Mua ngay (Buy-out)** | Kết thúc phiên ngay lập tức tại mức giá mua ngay |
| **Đăng sản phẩm** | Seller tạo phiên đấu giá với giá khởi điểm, buy-out price, tick size, thời hạn, bật/tắt anti-sniping |
| **Lịch sử giá thầu** | Xem toàn bộ lịch sử đặt giá của một phiên đấu giá |
| **Lịch sử hoạt động cá nhân** | Theo dõi các thông báo hoạt động của phiên làm việc hiện tại |
| **Chống bắn tỉa (Anti-Sniping)** | Tự động gia hạn thêm 30 giây nếu có bid trong 30 giây cuối |

### Quản trị viên (Admin)

| Tính năng | Mô tả |
|---|---|
| **Quản lý người dùng** | Xem danh sách, khoá (ban) tài khoản người dùng vi phạm |
| **Quản lý phiên đấu giá** | Xem danh sách tất cả phiên đang diễn ra và đã kết thúc |
| **Gỡ bỏ sản phẩm** | Xoá/ban một sản phẩm khỏi hệ thống (cả ACTIVE lẫn ENDED/SOLD) |

### Hệ thống (Server-side)

| Tính năng | Mô tả |
|---|---|
| **Tự động kết thúc đấu giá** | `AuctionScheduler` kiểm tra mỗi giây, tự động chốt kết quả phiên hết hạn |
| **Thông báo real-time** | Server broadcast `AUCTION_ENDED`, `AUCTION_SOLD`, `YOU_WON`, `UPDATE_BID` tới đúng client |
| **Push Notification tập trung** | `UserPushUpNotificationController` xử lý tất cả thông báo pop-up phía client |
| **Đồng bộ RAM–Database** | Phiên ACTIVE sống trên RAM; bị lỗi DB thì tự hoàn tác (`revertLastBid` / `revertBuyOut`) |
| **Multi-room auction** | Nhiều phiên đấu giá diễn ra song song, độc lập và an toàn nhờ lock per-auction |

---

## Kiến trúc & Thiết kế OOP

### Mô hình Client-Server

```
┌─────────────────┐         TCP/IP Socket          ┌─────────────────┐
│  auction-client │◄──────────────────────────────►│ auction-server  │
│  (JavaFX GUI)   │            Port 8080            │ (Business Logic)│
│                 │   ┌──────────────────┐          │                 │
│  Controllers    │   │  auction-shared  │          │ Entities        │
│  ClientService  │◄─►│ (DTOs, Network)  │◄────────►│ DAOs / Services │
│  UserSession    │   └──────────────────┘          │ AuctionManager  │
└─────────────────┘                                 │ AuctionScheduler│
                                                    │ ClientHandler   │
                                                    └────────┬────────┘
                                                             │ HikariCP
                                                             ▼
                                                    ┌─────────────────┐
                                                    │   PostgreSQL    │
                                                    │   (AWS RDS)     │
                                                    └─────────────────┘
```

### Mô hình 3 lớp (Server)

```
┌──────────────────────────────────────────────────────────┐
│  Network Layer  — SocketServer, ClientHandler            │
│  (Nhận/gửi NetworkMessage qua TCP; Virtual Threads)      │
├──────────────────────────────────────────────────────────┤
│  Business Logic — BidService, UserService, SellService   │
│  ValidatorService, AuctionManager, AuctionScheduler      │
│  (Luật đấu giá, lock đồng thời, vòng đời phiên đấu giá) │
├──────────────────────────────────────────────────────────┤
│  Data Access Layer — UserDAO, ItemDAO,                   │
│  AuctionDAO, BidTransactionDAO (via HikariCP)            │
│  (CRUD, JDBC Transactions, Connection Pool)              │
└──────────────────────────────────────────────────────────┘
```

### Cơ chế đồng thời & Khoá (Concurrency & Lock)

```
BidService.processAndSaveBid(auction, tx):
  1. auction.getLock().lock()          ← Khóa độc quyền phiên đấu giá
  2. auction.prepareBidInMemory(tx)    ← Validate + cập nhật RAM
  3. conn = getConnection()
     conn.setAutoCommit(false)
     bidDAO.insert(tx, conn)
     auctionDAO.updateHighestBid(conn)
     conn.commit()                     ← Ghi DB thành công
  4. [Nếu lỗi DB] conn.rollback()
                  auction.revertLastBid(tx)  ← Hoàn tác RAM
  5. finally: auction.getLock().unlock()
```

### Các Design Patterns sử dụng

| Pattern | Ví dụ triển khai |
|---|---|
| **Singleton** | `AuctionManager`, `ClientService`, `UserSession`, `DatabaseConnection` |
| **Factory** | `ItemFactory` → `ArtsFactory`, `ElectronicsFactory`, `VehiclesFactory` |
| **DAO** | `UserDAO`, `ItemDAO`, `AuctionDAO`, `BidTransactionDAO` |
| **Observer / Listener** | `AuctionUpdateListener` — Client nhận cập nhật real-time từ server |
| **Strategy / Behavior** | `BidderProfile`, `SellerProfile`, `AdminProfile` |
| **Transaction Script** | `BidService` — Xử lý luồng bid/buyout với lock + DB transaction + rollback |
| **Virtual Threads** | `SocketServer` dùng `newVirtualThreadPerTaskExecutor` cho đồng thời cao |

### Entity Hierarchy

```
Entity (abstract)
├── User
│   ├── BidderProfile (behavior)
│   ├── SellerProfile (behavior)
│   └── AdminProfile  (behavior)
├── Item<T> (abstract, generic)
│   ├── Arts         (typeSpecificAttribute: String — tên nghệ sĩ)
│   ├── Electronics  (typeSpecificAttribute: String — thông số kỹ thuật)
│   └── Vehicles     (typeSpecificAttribute: String — biển số xe)
├── Auction           (quản lý trạng thái, bidHistory, ReentrantLock)
└── BidTransaction    (lưu lịch sử mỗi lượt trả giá)
```

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| **Ngôn ngữ** | Java | 21 |
| **Build Tool** | Apache Maven | 3.8+ |
| **Client UI** | JavaFX | 23.0.1 |
| **Icon Library** | Ikonli (FontAwesome5) | 12.4.0 |
| **Networking** | Java TCP/IP Socket + Virtual Threads | (built-in Java 21) |
| **Database** | PostgreSQL | (AWS RDS) |
| **Connection Pool** | HikariCP | 5.1.0 |
| **JDBC Driver** | PostgreSQL JDBC | 42.7.3 |
| **Logging** | Logback + SLF4J | 1.5.32 / 2.0.16 |
| **Unit Testing** | JUnit 5 + Mockito | 5.x |

---

## Cấu trúc Dự án

```
Auction-System_Team5/
├── pom.xml                            # Parent POM (multi-module Maven)
│
├── auction-shared/                    # Thư viện dùng chung (client ↔ server)
│   └── src/main/java/
│       ├── module-info.java           # Khai báo JPMS module
│       └── auctionshared/
│           ├── dto/                   # AuctionDTO, ItemDTO, UserDTO,
│           │                          # BidTransactionDTO, AuctionStatus, SignUpDTO
│           └── Network/               # NetworkMessage, Notification
│
├── auction-server/                    # Ứng dụng Server
│   └── src/main/java/auctionserver/
│       ├── Main.java                  # Entry point: Rebuild RAM từ DB → Scheduler → Socket
│       ├── base/Entity.java           # Base entity (abstract)
│       ├── entities/                  # Auction, BidTransaction, Item<T>, User
│       │   └── items/                 # Arts, Electronics, Vehicles
│       ├── behaviors/                 # BidderBehaviors, SellerBehaviors, AdminBehaviors
│       │   └── profile/               # BidderProfile, SellerProfile, AdminProfile (interfaces)
│       ├── core/                      # AuctionManager (Singleton), AuctionScheduler
│       ├── dao/                       # UserDAO, ItemDAO, AuctionDAO, BidTransactionDAO
│       │                              # DAOProvider, DatabaseConnection (HikariCP)
│       ├── exception/                 # BidException, InactiveBidException,
│       │                              # InvalidBidAmountException, SelfBiddingException,
│       │                              # DatabaseException, TransactionFailedException, ...
│       ├── factory/                   # ItemFactory, ArtsFactory, ElectronicsFactory, VehiclesFactory
│       ├── interfaces/                # Các interface DAO & behavior
│       ├── mapper/                    # Mappers (Entity ↔ DTO)
│       ├── Network/                   # SocketServer, ClientHandler
│       └── service/                   # BidService, UserService, SellService
│                                      # ValidatorService, MessageHandlerService
│   └── src/test/java/auctionserver/
│       ├── core/                      # AuctionManagerTest, AuctionSchedulerTest
│       ├── entities/                  # AuctionTest
│       ├── factory/                   # ItemFactoryTest
│       ├── mapper/                    # MappersTest
│       └── service/                   # BidServiceTest, ValidatorServiceTest,
│                                      # MessageHandlerServiceTest
│
├── auction-client/                    # Ứng dụng Client (JavaFX)
│   └── src/main/java/auctionclient/
│       ├── launcher/                  # Launcher.java (đăng ký global AuctionUpdateListener)
│       ├── Network/                   # ClientService.java (singleton socket client)
│       ├── UserSession.java           # Session singleton (lưu thông tin người dùng)
│       └── controllers/
│           ├── auth/                  # LoginController, RegisterController
│           ├── main/                  # MainController (layout chính)
│           ├── bidder/                # AllProductController, MyActivitiesController,
│           │                          # BidHistoryController
│           ├── seller/                # SellerController
│           ├── admin/                 # AdminControlPanelController
│           └── notification/          # UserPushUpNotificationController (push-up alerts)
│   └── src/main/resources/auctionclient/
│       ├── fxml/                      # Các file layout FXML
│       └── images/                    # Assets (logo, icons)
│
└── README.md
```

---

## Hướng dẫn Cài đặt & Chạy chương trình

### Yêu cầu môi trường

| Phần mềm | Phiên bản tối thiểu | Ghi chú |
|---|---|---|
| **JDK** | 21 | Hỗ trợ Virtual Threads & Pattern Matching |
| **Apache Maven** | 3.8+ | Quản lý build và dependency |
| **Git** | Bất kỳ | Để clone dự án |
| **PostgreSQL** | 13+ | Hoặc kết nối vào AWS RDS đã cấu hình sẵn |

### Bước 1 — Clone dự án

```bash
git clone https://github.com/NVQuangMinh/Auction-System_Team5.git
cd Auction-System_Team5
```

### Bước 2 — Cấu hình kết nối Database

Mở file cấu hình kết nối của server và điền thông tin database của bạn.
Tìm class `DatabaseConnection.java` tại:

```
auction-server/src/main/java/auctionserver/dao/DatabaseConnection.java
```

Hoặc thiết lập biến môi trường (khuyến nghị cho Production):

**Linux / macOS:**
```bash
export DB_URL="jdbc:postgresql://<host>:5432/<database>"
export DB_USER="<username>"
export DB_PASSWORD="<password>"
```

**Windows (PowerShell):**
```powershell
$env:DB_URL = "jdbc:postgresql://<host>:5432/<database>"
$env:DB_USER = "<username>"
$env:DB_PASSWORD = "<password>"
```

### Bước 3 — Build toàn bộ dự án

Chạy lệnh này từ thư mục gốc dự án (`Auction-System_Team5/`):

**Linux / macOS / Windows:**
```bash
mvn clean install -DskipTests
```

> Lệnh này sẽ build cả 3 module theo đúng thứ tự phụ thuộc: `auction-shared` → `auction-server` → `auction-client`.

### Bước 4 — Chạy Server *(BẮT BUỘC chạy trước)*

Server cần khởi động **trước** để sẵn sàng nhận kết nối từ Client.

```bash
cd auction-server
mvn exec:java -Dexec.mainClass="auctionserver.Main"
```

Khi khởi động thành công, console sẽ hiển thị:
```
[Hệ thống] đã tải N phiên đấu giá đang hoạt động từ cơ sở dữ liệu.
[Hệ thống] Auction Scheduler đã bắt đầu.
[Hệ thống] Khởi động Socket Server...
Server đã khởi động tại địa chỉ: 8080
```

> Server lắng nghe tại **port 8080**.

### Bước 5 — Chạy Client

Mở một cửa sổ terminal/command prompt mới, sau đó chạy:

```bash
cd auction-client
mvn javafx:run
```

> **Lưu ý:** Bạn có thể mở nhiều cửa sổ Client đồng thời để kiểm thử chức năng đấu giá nhiều người chơi.

**Tài khoản mặc định để thử nghiệm:**

| Vai trò | Username | Password |
|---|---|---|
| Admin | *(xem trong DB)* | *(xem trong DB)* |
| User | *(tạo mới qua màn hình Đăng ký)* | — |

---

## Danh sách chức năng đã hoàn thành

- [x] Đăng ký / Đăng nhập / Đăng xuất (phân quyền theo vai trò)
- [x] Xem danh sách phiên đấu giá đang diễn ra (ACTIVE - real-time)
- [x] Xem danh sách phiên đấu giá đã kết thúc (ENDED/SOLD - từ DB)
- [x] Đặt giá thầu (Place Bid) với xác thực tick size, chống tự mua, kiểm tra trạng thái phiên
- [x] Mua ngay (Buy-out) kết thúc phiên tức thì
- [x] Anti-Sniping: tự động gia hạn 30 giây nếu có bid trong 30 giây cuối
- [x] Đăng sản phẩm (Seller) với các loại: Arts, Electronics, Vehicles
- [x] Xem lịch sử giá thầu của một phiên
- [x] Xem danh sách sản phẩm cá nhân đã đăng bán
- [x] Thông báo real-time: `AUCTION_ENDED`, `AUCTION_SOLD`, `YOU_WON`, `UPDATE_BID`
- [x] Push Notification tập trung qua `UserPushUpNotificationController`
- [x] Tự động kết thúc phiên hết hạn qua `AuctionScheduler` (mỗi giây)
- [x] Khôi phục phiên đấu giá từ Database khi Server khởi động lại
- [x] Admin: Quản lý người dùng, khoá tài khoản (ban)
- [x] Admin: Gỡ bỏ/ban sản phẩm (cả ACTIVE lẫn ENDED/SOLD)
- [x] Transaction an toàn: Rollback DB + Revert RAM nếu có sự cố
- [x] Đồng bộ lock (ReentrantLock per-auction) đảm bảo tính toàn vẹn đồng thời

---

## Tài liệu & Demo

| Loại | Link |
|---|---|
| **Báo cáo PDF** | *(Thêm link vào đây)* |
| **Video Demo** | *(Thêm link vào đây)* |

---

*Được phát triển bởi Team 5 — Môn Lập trình nâng cao.*
