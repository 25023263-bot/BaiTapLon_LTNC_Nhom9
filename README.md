# 🏷️ Auction House — Hệ thống đấu giá trực tuyến

> Bài tập lớn môn Lập trình Nâng cao — Nhóm 9

---

## 1. Mô tả bài toán

Auction House là ứng dụng đấu giá trực tuyến theo mô hình **Client–Server**, cho phép nhiều người dùng tham gia đặt giá đồng thời trên cùng một phiên đấu giá theo thời gian thực.

**Phạm vi hệ thống:**

- **Buyer** — duyệt sản phẩm, đặt giá thủ công hoặc kích hoạt Auto-Bid, quản lý ví tiền.
- **Seller** — đăng sản phẩm lên đấu giá, theo dõi trạng thái phiên, xem lịch sử.
- **Admin** — quản lý toàn bộ người dùng và sản phẩm, xem doanh thu nền tảng.
- **Server** — xử lý logic đấu giá, đồng bộ trạng thái đến tất cả Client qua Socket.

---

## 2. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 25 |
| UI | JavaFX 21 + FXML |
| Database | SQLite (via `sqlite-jdbc 3.45.1`) |
| Connection Pool | HikariCP 5.1.0 |
| Mã hóa mật khẩu | jBCrypt (12 rounds) |
| Real-time | Java Socket (TCP, port **9999**) |
| Build tool | Maven 3 (Maven Shade Plugin — fat JAR) |
| Unit Test | JUnit 5.12.1 + Mockito |
| CI/CD | GitHub Actions (push/PR → `mvn test`) |

**Môi trường chạy:** Windows / macOS / Linux có cài **JDK 25+**.

---

## 3. Yêu cầu cài đặt

```
JDK 25 trở lên     https://adoptium.net
Maven 3.9+         https://maven.apache.org  (hoặc dùng ./mvnw đi kèm)
```

Không cần cài thêm database — SQLite được nhúng sẵn trong JAR, file `auction.db` tự tạo khi chạy lần đầu.

---

## 4. Cấu trúc module chính

```
src/main/java/.../
├── config/
│   └── AppConfig.java              # Hằng số toàn cục (port, phí, timeout…)
│
├── domain/
│   ├── model/
│   │   ├── user/                   # User (abstract) → Buyer / Seller / Admin
│   │   ├── item/                   # AuctionItem (abstract) → PhysicalItem
│   │   ├── Bid.java
│   │   └── Notification.java
│   ├── dto/                        # UserDTO, ItemDTO, BidDTO (an toàn truyền mạng)
│   └── model/enums/                # AuctionStatus, UserRole
│
├── exception/                      # 5 custom exception có payload chi tiết
│
├── repository/                     # DAO truy cập SQLite thuần JDBC
│   ├── UserRepository.java
│   ├── AuctionRepository.java
│   ├── BidRepository.java
│   └── NotificationRepository.java
│
├── service/
│   ├── DatabaseConnection.java     # Singleton HikariCP pool
│   ├── DbUtil.java                 # Tiện ích datetime/timestamp SQLite
│   ├── auction/
│   │   ├── AuctionHouse.java       # Logic đấu giá (synchronized, auto-bid, anti-snipe)
│   │   ├── AuctionScheduler.java   # Daemon thread: tự mở/đóng phiên
│   │   ├── AuctionObserver.java    # Interface Observer pattern
│   │   └── ServiceLocator.java     # Singleton registry các service
│   ├── auth/                       # AuthService, PasswordHasher, Authenticatable
│   ├── listing/                    # ListingService, ListingRequest
│   ├── notification/               # NotificationService (in-memory inbox)
│   └── wallet/                     # WalletDepositService
│
├── server/
│   ├── AuctionServer.java          # Entry point Server — mở ServerSocket port 9999
│   ├── ClientHandler.java          # Mỗi client → 1 thread riêng
│   ├── SocketNotifier.java         # Implements AuctionObserver → broadcast realtime
│   └── protocol/                   # Request / Response (serializable)
│
├── client/
│   └── SocketClient.java           # Kết nối server, BlockingQueue tránh race condition
│
└── ui/
    ├── controller/                 # JavaFX Controllers (Login, Home, ItemDetail…)
    ├── presenter/                  # MVP Presenter (Admin, Catalog, Profile…)
    ├── coordinator/                # Điều phối luồng màn hình
    ├── factory/                    # ProductCardFactory
    ├── helpers/                    # UserSession, AlertHelper, CurrencyFormatHelper
    └── network/                    # ServerConnection (UI ↔ Socket)

src/main/resources/
├── fxml/                           # Tất cả file giao diện FXML
├── css/                            # Stylesheet cho từng màn hình
└── db/
    ├── schema.sql                  # DDL SQLite — tự chạy khi khởi động
    └── seed.sql                    # Dữ liệu mẫu (tùy chọn)
```

---

## 5. Vị trí file JAR

Sau khi build (`mvn package`), hai file JAR được tạo tại:

```
target/
├── server.jar   ← chạy Server (entry: AuctionServer)
└── client.jar   ← chạy Client/UI (entry: Launcher → HelloApplication)
```

---

## 6. Hướng dẫn chạy

### Bước 1 — Build

```bash
mvn package -DskipTests
```

### Bước 2 — Chạy Server **trước**

```bash
java -jar target/server.jar
```

Khi thấy dòng log:
```
=== Auction Server đang chạy tại cổng 9999 ===
```
thì Server đã sẵn sàng.

> Server đọc host/port từ `server.properties` (mặc định `localhost:9999`).
> Để nhiều máy kết nối, thay `server.host=<IP máy chủ>` trong file đó.

### Bước 3 — Chạy Client (có thể mở nhiều cửa sổ)

```bash
java -jar target/client.jar
```

> Client tự kết nối đến host/port khai báo trong `server.properties`.
> Mỗi lần chạy `client.jar` là một phiên người dùng độc lập — có thể chạy song song để test đấu giá đồng thời.

### Tài khoản admin mặc định (seed.sql)

| Role | Username | Password |
|---|---|----|
| Admin | `admin` | `12345678Aa` |
| Seller | `` | `` |
| Buyer | `` | `` |

---

## 7. Chức năng đã hoàn thành

### Bắt buộc

- [x] **Quản lý người dùng** — đăng ký, đăng nhập, cập nhật hồ sơ, phân quyền Buyer / Seller / Admin
- [x] **Quản lý sản phẩm** — Seller đăng sản phẩm lên đấu giá, đặt giá khởi điểm và thời gian kết thúc
- [x] **Chức năng đấu giá** — đặt giá, kiểm tra giá tối thiểu, kiểm tra số dư ví, xác nhận thanh toán
- [x] **Xử lý lỗi & ngoại lệ** — 5 custom exception: `AuctionClosedException`, `BidTooLowException`, `InsufficientBalanceException`, `AuthenticationException`, `DuplicateUserException`
- [x] **Đấu giá đồng thời an toàn** — `synchronized` trên các method đặt giá, commit/rollback DB, `CopyOnWriteArrayList` cho danh sách client
- [x] **Realtime update** — Observer + Socket: mỗi bid mới được broadcast ngay đến tất cả client đang xem phiên đó
- [x] **Kiến trúc Client–Server** — Server socket TCP port 9999, mỗi client một thread riêng
- [x] **MVC / MVP** — JavaFX + FXML (View), Controller + Presenter (Controller), Repository (DAO)
- [x] **Maven & coding convention** — package rõ ràng, Javadoc đầy đủ, không có magic number
- [x] **Unit Test (JUnit + Mockito)** — 11 file test, ~2800 dòng, cover AuctionHouse, AuthService, WalletDepositService, model, exception
- [x] **CI/CD** — GitHub Actions: mỗi push/PR tự động chạy `mvn test`

### Nâng cao

- [x] **Auto-Bidding** — Buyer đặt `maxBid` ; hệ thống tự bid khi bị vượt, giải quyết xung đột hai auto-bid thông minh (tránh tạo row thừa trong DB)
- [x] **Anti-Sniping** — tự động gia hạn thêm 60 giây nếu có bid trong 60 giây cuối phiên
- [x] **Bid History Visualization** — biểu đồ đường giá realtime trong màn hình chi tiết sản phẩm, cập nhật ngay khi có bid mới qua Observer

---

## 8. Báo cáo & Demo

| | Link |
|---|---|
| 📄 Báo cáo PDF | _[Thêm link tại đây]_ |
| 🎬 Video demo | _[Thêm link tại đây]_ |

---

## 9. Thành viên nhóm 9

| Họ tên            | MSSV     | 
|-------------------|----------|
| _Nguyễn Văn Hiệp_ | 25023241 |
| _Nguyễn Phi Hùng_ |          |
| _Vũ Thị Bích Hợp_ |          |
| _Nguyễn Đức Minh_ |          |