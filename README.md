# Auction-System_Team5

<div align="center">

![Auction System Logo](./auction-client/src/main/resources/auction_client/images/Auction-System.png)

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
- [Hướng dẫn Cài đặt](#hướng-dẫn-cài-đặt)

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
- **Virtual Threads (Java 21)** — Server xử lý hàng trăm kết nối đồng thời mà không phí tài nguyên.
- **Transaction & Rollback** — Logic đặt giá thầu được bảo vệ bởi transaction, đảm bảo tính nhất quán dữ liệu.

---

## Tính năng Chính

### Người dùng

| Tính năng | Mô tả |
|---|---|
| **Đăng nhập / Đăng ký** | Xác thực người dùng qua Socket; phân quyền tự động theo vai trò |
| **Duyệt sản phẩm** | Xem danh sách sản phẩm theo danh mục: Arts, Electronics, Vehicles |
| **Đặt giá thầu (Place Bid)** | Ra giá cao hơn giá hiện tại; tick size được thiết lập bởi người bán |
| **Mua ngay (Buy-out)** | Thanh toán ngay lập tức tại mức giá buy-out, kết thúc đấu giá |
| **Đăng sản phẩm** | Seller tạo phiên đấu giá với giá khởi điểm, buy-out price, tick size, thời hạn |
| **Lịch sử hoạt động** | Xem toàn bộ hoạt động đấu giá cá nhân |

### Quản trị viên

| Tính năng | Mô tả |
|---|---|
| **Bảng điều khiển Admin** | Quản lý người dùng, giám sát phiên đấu giá đang hoạt động |

### Hệ thống

| Tính năng | Mô tả |
|---|---|
| **Tự động kết thúc đấu giá** | Scheduler kiểm tra mỗi giây, tự động đóng phiên hết hạn |
| **Thông báo real-time** | Thông báo `YOU_WON`, `AUCTION_ENDED`, `UPDATE_BID` tới client |
| **Multi-room auction** | Nhiều phiên đấu giá diễn ra song song |

---

## Kiến trúc & Thiết kế OOP

### Mô hình Client-Server

```
┌─────────────────┐         TCP/IP Socket          ┌─────────────────┐
│  auction-client │◄──────────────────────────────►│ auction-server  │
│  (JavaFX GUI)   │         Port 8080              │ (Business Logic)│
│                 │   ┌──────────────────┐          │                 │
│  Controllers     │   │ auction-shared  │          │ Entities        │
│  ClientService   │◄─►│ (DTOs, Network)│◄────────►│ DAOs / Services │
│  UserSession     │   └──────────────────┘          │ AuctionManager  │
└─────────────────┘                                  │ ClientHandler   │
                                                     │ AuctionScheduler│
                                                     └────────┬────────┘
                                                              │ HikariCP
                                                              ▼
                                                     ┌─────────────────┐
                                                     │ PostgreSQL      │
                                                     │ (AWS RDS)       │
                                                     └─────────────────┘
```

### Mô hình 3 lớp (Server)

```
┌──────────────────────────────────────────────────────┐
│  Presentation Layer  — ClientHandler, SocketServer   │
│  (Network I/O, deserialize request, serialize reply) │
├──────────────────────────────────────────────────────┤
│  Business Logic Layer — BidService, UserService,     │
│  AuctionManager, AuctionScheduler                     │
│  (Bidding rules, auction lifecycle, scheduling)      │
├──────────────────────────────────────────────────────┤
│  Data Access Layer — DAOs (UserDAO, ItemDAO,        │
│  AuctionDAO, BidTransactionDAO)                       │
│  (CRUD, transactions, connection pooling)             │
└──────────────────────────────────────────────────────┘
```

### Các Design Patterns sử dụng

| Pattern | Ví dụ triển khai |
|---|---|
| **Singleton** | `AuctionManager`, `ClientService`, `UserSession`, `DatabaseConnection` |
| **Factory** | `ItemFactory` → `ArtsFactory`, `ElectronicsFactory`, `VehiclesFactory` |
| **DAO** | `UserDAO`, `ItemDAO`, `AuctionDAO`, `BidTransactionDAO` |
| **Observer / Listener** | `AuctionUpdateListener` — client nhận cập nhật real-time từ server |
| **Transaction** | `BidService` — đặt giá thầu bên trong DB transaction, rollback nếu thất bại |
| **Virtual Threads** | `SocketServer` dùng virtual-thread executor cho đồng thời cao |

### Entity Hierarchy

```
Entity (abstract)
├── User
│   ├── BidderProfile (role)
│   ├── SellerProfile (role)
│   └── AdminProfile  (role)
├── Item (abstract)
│   ├── Arts
│   ├── Electronics
│   └── Vehicles
├── Auction
└── BidTransaction
```

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| **Ngôn ngữ** | Java | 21 |
| **Build Tool** | Apache Maven | 3.x |
| **Client UI** | JavaFX | 23.0.1 |
| **Icon Library** | Ikonli (FontAwesome5) | 12.4.0 |
| **Networking** | Java TCP/IP Socket | (built-in) |
| **Database** | PostgreSQL | (AWS RDS) |
| **Connection Pool** | HikariCP | 5.1.0 |
| **JDBC Driver** | PostgreSQL | 42.7.3 |
| **Logging** | Logback + SLF4J | 1.5.32 / 2.0.16 |

---

## Cấu trúc Dự án

```
Auction-System_Team5/
├── pom.xml                        # Parent POM (multi-module)
│
├── auction-shared/                # Shared library (client ↔ server)
│   └── src/main/java/
│       ├── auction_shared/
│       │   ├── dto/               # DTOs: AuctionDTO, ItemDTO,
│       │   │                       # UserDTO, BidTransactionDTO, ...
│       │   └── Network/           # NetworkMessage, Notification
│       └── pom.xml
│
├── auction-server/                # Server application
│   └── src/main/java/auction_server/
│       ├── Main.java              # Entry point: khởi động server
│       ├── base/Entity.java       # Base entity class
│       ├── entities/              # Domain entities
│       ├── dao/                   # Data Access Objects
│       ├── service/               # Business logic services
│       ├── core/                  # AuctionManager, AuctionScheduler
│       ├── Network/               # SocketServer, ClientHandler
│       ├── behaviors/             # AdminProfile, BidderProfile, SellerProfile
│       ├── factory/               # ItemFactory pattern
│       └── interfaces/            # DAO & behavior contracts
│   └── pom.xml
│
├── auction-client/                # JavaFX client application
│   └── src/main/java/auction_client/
│       ├── launcher/              # JavaFX Application entry points
│       ├── controllers/          # FXML controllers
│       ├── Network/               # ClientService (socket client)
│       └── UserSession.java      # Session singleton
│   └── src/main/resources/
│       ├── auction_client/
│       │   ├── fxml/             # *.fxml layout files
│       │   └── images/           # Assets
│       └── logback.xml
│   └── pom.xml
│
└── README.md
```

---

## Hướng dẫn Cài đặt

### Yêu cầu

- **Java Development Kit (JDK) 21** trở lên
- **Apache Maven 3.8+**
- **PostgreSQL** (cục bộ hoặc AWS RDS)
- **Git**

### Các bước

**1. Clone dự án**

```bash
git clone https://github.com/<your-org>/Auction-System_Team5.git
cd Auction-System_Team5
```

**2. Cấu hình database**

Tạo file `auction-server/src/main/resources/application.properties` hoặc thiết lập biến môi trường:

```bash
export DB_URL="jdbc:postgresql://<host>:5432/<database>"
export DB_USER="<username>"
export DB_PASSWORD="<password>"
```

**3. Build toàn bộ dự án**

```bash
mvn clean install
```

**4. Chạy Server**

```bash
cd auction-server
mvn exec:java -Dexec.mainClass="auction_server.Main"
```

Server khởi động SocketServer tại **port 8080** và AuctionScheduler kiểm tra phiên đấu giá mỗi giây.

**5. Chạy Client**

```bash
cd auction-client
mvn javafx:run
```

> **Lưu ý:** Client yêu cầu JavaFX runtime. Nếu gặp lỗi module, đảm bảo `--add-modules` được cấu hình đúng trong `pom.xml` của client.

---

*Được phát triển bởi Team 5 — Môn Lập trình nâng cao.*
