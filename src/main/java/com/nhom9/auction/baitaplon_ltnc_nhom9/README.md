1. Config
AppConfig — file duy nhất chứa mọi hằng số toàn cục: đường dẫn DB (auction.db), phí nền tảng 2%, thời gian gia hạn phút cuối (60 giây), kích thước trang mặc định (12), tên app. Bất kỳ số "magic" nào trong code đều phải lấy từ đây thay vì viết cứng.

2. Domain
Đây là lõi nghiệp vụ thuần túy — không phụ thuộc framework, không phụ thuộc DB.
Model – User (4 class): User là abstract base với activate/deactivate/updateProfile/changePassword. Buyer thêm ví tiền với deposit/deduct/hasSufficientBalance. Seller thêm earningsBalance, hệ thống rating tính lại trung bình động. Admin thêm accessLevel và isSuperAdmin.
Model – Item (3 class): AuctionItem là abstract với logic isValidBid, getNextMinimumBid, getRemainingSeconds, extendIfLastMinute. PhysicalItem thêm condition/shipping/pickup. DigitalItem thêm deliveryContent ẩn cho đến khi thanh toán.
Model – Giao dịch (2 class): Bid chứa auto-bid logic với canAutoBidUp và calculateAutoBidAmount. Transaction tự tính platformFee, sellerReceives, totalPaid và có các method markCompleted/Failed/Refunded.
Common (2 class): FilterCriteria dùng Builder pattern với 10+ điều kiện lọc. Page<T> là generic pagination với offset, hasNextPage, isLastPage.
Enums (3): AuctionStatus (5 trạng thái: PENDING/ACTIVE/CLOSED/EXPIRED/CANCELLED), UserRole, PaymentStatus.
DTO + Mapper (6 class): DTO không chứa passwordHash hay deliveryContent — an toàn truyền ra ngoài. Mapper chuyển đổi hai chiều, BidMapper.toDTOListWithLeading đánh dấu bid đang dẫn đầu.

3. Exception
5 custom exception với thông tin chi tiết để UI hiển thị đúng: InsufficientBalanceException mang theo số dư có và số cần; BidTooLowException mang theo bid amount và minimum; AuctionClosedException mang theo currentStatus; AuthenticationException có enum Reason (INVALID_CREDENTIALS, ACCOUNT_DISABLED, SESSION_EXPIRED); DuplicateUserException phân biệt USERNAME vs EMAIL.

4. Repository
5 DAO truy cập SQLite trực tiếp qua JDBC, không dùng ORM.
UserRepository — dùng table-per-subclass: bảng users + bảng phụ buyers/sellers/admins. Mỗi lần load tự join đúng bảng theo role. Có updateWalletBalance và updateEarningsBalance riêng để tránh update toàn bộ row.
ItemRepository — tương tự với auction_items + physical_items/digital_items. Quan trọng nhất là search() build SQL động từ FilterCriteria với COUNT + DATA query tách biệt để phân trang. Có findExpiredActive và findDueToStart cho Scheduler.
BidRepository — findLeadingBid lấy bid cao nhất, findAutoBid lấy auto-bid còn hiệu lực của một buyer, tất cả JOIN với users để lấy username mà không cần query thêm.
WatchlistRepository — đơn giản nhất, dùng INSERT OR IGNORE cho unique constraint.
TransactionRepository — có totalPlatformRevenue cho Admin dashboard.

5. Service
AuthService — validate input client-side (regex username, format email, độ mạnh password) trước khi query DB, BCrypt hash với 12 rounds.
AuctionHouse — synchronized để thread-safe khi nhiều bid đồng thời. Logic auto-bid counter: sau mỗi bid thắng, quét tìm auto-bid của đối thủ và kích hoạt nếu còn trong giới hạn. Gia hạn phiên nếu có bid trong 60 giây cuối. processPayment là atomic: trừ ví buyer → tạo transaction → cộng tiền seller → tăng win count → mark completed.
AuctionScheduler — daemon thread pool, poll mỗi 10 giây, tự mở PENDING→ACTIVE và đóng ACTIVE hết giờ mà không block UI thread.
NotificationService — implement AuctionObserver, nhận event từ AuctionHouse, lưu vào in-memory inbox per user, UI đăng ký callback để nhận real-time.
WalletPayment / CreditCardPayment — implement interface PaymentMethod. Credit card là stub sẵn hook tích hợp gateway thật.
SearchService — implement Searchable, bọc ItemRepository.search(), thêm suggest (autocomplete) và getActiveCategories.

6. UI Helpers
UserSession — Singleton giữ User đang đăng nhập, có guard requireLogin() ném exception nếu chưa login.
AlertHelper — bọc JavaFX Alert với các method tĩnh showInfo/Warning/Error/Confirm/Exception. showException hiển thị stack trace có thể expand.
DateTimeUtils — formatCountdown trả "2 ngày 3 giờ", formatCountdownShort trả "01:23:45", getCountdownStyleClass trả CSS class để đổi màu đếm ngược (xanh → vàng → đỏ), formatRelative trả "5 phút trước", formatCurrency format số tiền VND.