package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network;

import com.nhom9.auction.baitaplon_ltnc_nhom9.client.SocketClient;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Request;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Response;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing.ListingRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Facade tập trung mọi lời gọi socket từ client → server.
 *
 * <p>Thay vì để từng Controller gọi {@link SocketClient#sendRequest} trực tiếp,
 * toàn bộ "giao tiếp mạng" đi qua class này. Lợi ích:
 * <ul>
 *   <li>Một chỗ duy nhất để sửa khi thay đổi protocol (request type, payload format).</li>
 *   <li>Controller chỉ biết "tôi cần getAuctions()" — không cần biết socket hay HTTP.</li>
 *   <li>Dễ mock trong unit test.</li>
 * </ul>
 *
 * <p><b>Luồng gọi chuẩn từ Controller:</b>
 * <pre>
 *   new Thread(() -> {
 *       try {
 *           List&lt;AuctionItem&gt; items = ServerConnection.getAuctions();
 *           Platform.runLater(() -> { /* cập nhật UI *&#47; });
 *       } catch (Exception e) {
 *           Platform.runLater(() -> AlertHelper.showError(...));
 *       }
 *   }).start();
 * </pre>
 *
 * <p><b>Quan trọng:</b> Không gọi các method này trực tiếp trên JavaFX Application Thread —
 * chúng block cho đến khi server phản hồi (tối đa 30 giây).
 */
public class ServerConnection {

    private static final Logger LOG = Logger.getLogger(ServerConnection.class.getName());

    // ── Kiểm tra kết nối ─────────────────────────────────────────────────────

    public static boolean isConnected() {
        return SocketClient.getInstance().isConnected();
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    /**
     * Đăng nhập qua socket.
     * Convention: password gửi trong field phone của UserDTO.
     *
     * @return UserDTO chứa thông tin user (không có passwordHash)
     * @throws Exception nếu sai mật khẩu, user không tồn tại, hoặc mất kết nối
     */
    public static UserDTO login(String username, String password) throws Exception {
        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setPhone(password); // phone = password (tạm thời)

        Response res = send(Request.Type.LOGIN, dto);
        requireOk(res);
        return (UserDTO) res.getData();
    }

    /**
     * Đăng ký tài khoản mới qua socket.
     *
     * @return thông báo thành công từ server
     */
    public static String register(UserDTO userDTO) throws Exception {
        Response res = send(Request.Type.REGISTER, userDTO);
        requireOk(res);
        return (String) res.getData();
    }

    /**
     * Đăng xuất — server stateless, chỉ log phía server.
     * Client tự xóa UserSession sau khi gọi xong.
     */
    public static void logout() throws Exception {
        send(Request.Type.LOGOUT, null);
        // Không cần check response — logout không thể thất bại
    }

    // ── Auction ───────────────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ danh sách phiên đấu giá dưới dạng ItemDTO (có bidCount thực).
     *
     * @return List&lt;ItemDTO&gt; (có thể rỗng, không null)
     */
    @SuppressWarnings("unchecked")
    public static List<ItemDTO> getAuctions() throws Exception {
        Response res = send(Request.Type.GET_AUCTIONS, null);
        requireOk(res);
        return (List<ItemDTO>) res.getData();
    }

    /**
     * Lấy chi tiết một phiên đấu giá, bao gồm số lượt bid và người dẫn đầu.
     *
     * @param auctionId ID phiên đấu giá
     * @return ItemDTO với đầy đủ thông tin
     */
    public static ItemDTO getAuctionDetail(int auctionId) throws Exception {
        Response res = send(Request.Type.GET_AUCTION_DETAIL, auctionId);
        requireOk(res);
        return (ItemDTO) res.getData();
    }

    /**
     * Đặt giá thủ công.
     *
     * @param auctionId ID phiên đấu giá
     * @param buyerId   ID người mua
     * @param amount    Số tiền đặt giá
     * @return Bid vừa được lưu
     */
    public static Bid placeBid(int auctionId, int buyerId, BigDecimal amount) throws Exception {
        BidDTO dto = new BidDTO();
        dto.setAuctionId(auctionId);
        dto.setBuyerId(buyerId);
        dto.setAmount(amount);

        Response res = send(Request.Type.PLACE_BID, dto);
        requireOk(res);
        return (Bid) res.getData();
    }

    /**
     * Đặt giá tự động (proxy bidding).
     * Server sẽ đặt ở mức tối thiểu ngay, rồi tự counter khi bị vượt qua.
     *
     * @param auctionId ID phiên đấu giá
     * @param buyerId   ID người mua
     * @param maxLimit  Giới hạn tối đa của auto-bid
     * @return Bid đầu tiên được đặt (ở mức tối thiểu, không phải maxLimit)
     */
    public static Bid placeAutoBid(int auctionId, int buyerId, BigDecimal maxLimit) throws Exception {
        BidDTO dto = new BidDTO();
        dto.setAuctionId(auctionId);
        dto.setBuyerId(buyerId);
        dto.setAmount(maxLimit);

        Response res = send(Request.Type.PLACE_AUTO_BID, dto);
        requireOk(res);
        return (Bid) res.getData();
    }

    /**
     * Mua ngay (Buy Now) — đặt bid bằng buyNowPrice và đóng phiên ngay lập tức.
     *
     * @param auctionId ID phiên đấu giá
     * @param buyerId   ID người mua
     * @return Bid vừa được lưu
     */
    public static Bid buyNow(int auctionId, int buyerId) throws Exception {
        BidDTO dto = new BidDTO();
        dto.setAuctionId(auctionId);
        dto.setBuyerId(buyerId);
        // amount không cần — server lấy buyNowPrice từ DB

        Response res = send(Request.Type.BUY_NOW, dto);
        requireOk(res);
        return (Bid) res.getData();
    }

    /**
     * Hủy phiên đấu giá.
     *
     * @param auctionId ID phiên đấu giá cần hủy
     * @param sellerId  ID người bán (-1 nếu gọi từ Admin, bỏ qua kiểm tra quyền)
     */
    public static void cancelAuction(int auctionId, int sellerId) throws Exception {
        int[] payload = {auctionId, sellerId};
        Response res = send(Request.Type.CANCEL_AUCTION, payload);
        requireOk(res);
    }

    /**
     * Admin force-close: không kiểm tra sellerId.
     */
    public static void cancelAuction(int auctionId) throws Exception {
        Response res = send(Request.Type.CANCEL_AUCTION, auctionId);
        requireOk(res);
    }

    /**
     * Seller cập nhật thông tin phiên đấu giá.
     * Chỉ gửi các trường được phép sửa: title, description, endTime, imageUrl.
     *
     * @param auctionId  ID phiên đấu giá
     * @param title      Tên mới
     * @param description Mô tả mới
     * @param endTime    Thời gian kết thúc mới
     * @param imageUrl   Đường dẫn ảnh mới (null hoặc empty = giữ nguyên)
     */
    public static void updateAuction(int auctionId, String title, String description,
                                     java.time.LocalDateTime endTime, String imageUrl) throws Exception {
        ItemDTO dto = new ItemDTO();
        dto.setId(auctionId);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setEndTime(endTime);
        dto.setImageUrl(imageUrl);

        Response res = send(Request.Type.UPDATE_AUCTION, dto);
        requireOk(res);
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    /**
     * Lấy danh sách thông báo của user.
     *
     * @param userId ID người dùng
     * @return List&lt;Notification&gt; (có thể rỗng, không null)
     */
    @SuppressWarnings("unchecked")
    public static List<com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification>
    getNotifications(int userId) throws Exception {
        Response res = send(Request.Type.GET_NOTIFICATIONS, userId);
        requireOk(res);
        return (List<com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification>) res.getData();
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     *
     * @param notificationId ID thông báo
     * @param userId         ID người dùng (để verify quyền)
     */
    public static void markNotificationRead(int notificationId, int userId) throws Exception {
        Response res = send(Request.Type.MARK_NOTIFICATION_READ, new int[]{notificationId, userId});
        requireOk(res);
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     *
     * @param userId ID người dùng
     */
    public static void markAllNotificationsRead(int userId) throws Exception {
        Response res = send(Request.Type.MARK_ALL_NOTIFICATIONS_READ, userId);
        requireOk(res);
    }

    /**
     * Xóa tất cả thông báo của user.
     *
     * @param userId ID người dùng
     */
    public static void clearNotifications(int userId) throws Exception {
        Response res = send(Request.Type.CLEAR_NOTIFICATIONS, userId);
        requireOk(res);
    }

    // ── Seller ────────────────────────────────────────────────────────────────

    /**
     * Seller đăng bán sản phẩm mới.
     *
     * @param listing Thông tin sản phẩm (title, category, price, endTime...)
     */
    public static void createListing(ListingRequest listing) throws Exception {
        Response res = send(Request.Type.CREATE_LISTING, listing);
        requireOk(res);
    }

    /**
     * Nâng cấp tài khoản Buyer → Seller sau khi đồng ý điều khoản.
     *
     * @param userId ID người dùng cần nâng cấp
     */
    public static void upgradeToSeller(int userId) throws Exception {
        Response res = send(Request.Type.UPGRADE_TO_SELLER, userId);
        requireOk(res);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /**
     * Admin lấy toàn bộ danh sách người dùng.
     *
     * @return List&lt;User&gt; (có thể rỗng, không null)
     */
    @SuppressWarnings("unchecked")
    public static List<User> getUsers() throws Exception {
        Response res = send(Request.Type.GET_USERS, null);
        requireOk(res);
        return (List<User>) res.getData();
    }

    /**
     * Admin khoá hoặc mở khoá tài khoản người dùng (toggle trạng thái active).
     * Server tự toggle — client không cần biết trạng thái hiện tại.
     *
     * @param userId ID người dùng cần khoá/mở khoá
     * @return Thông báo kết quả từ server ("Đã khoá..." hoặc "Đã mở khoá...")
     */
    public static String toggleUserLock(int userId) throws Exception {
        Response res = send(Request.Type.TOGGLE_USER_LOCK, userId);
        requireOk(res);
        return (String) res.getData();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    // ── Wallet ────────────────────────────────────────────────────────────────

    /**
     * Nạp tiền vào ví người dùng.
     * Convention: amount gửi trong field phone của UserDTO (dạng String số).
     *
     * @param userId ID người dùng
     * @param amount Số tiền nạp (phải > 0)
     */
    public static UserDTO depositWallet(int userId, BigDecimal amount) throws Exception {
        UserDTO dto = new UserDTO();
        dto.setId(userId);
        dto.setPhone(amount.toPlainString()); // phone = amount (convention)

        Response res = send(Request.Type.DEPOSIT_WALLET, dto);
        requireOk(res);
        // FIX: Server giờ trả về UserDTO chứa số dư mới — client dùng luôn, không tự tính.
        return (UserDTO) res.getData();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Gửi request và nhận response.
     * Tất cả lỗi mạng sẽ ném ra để caller xử lý.
     */
    private static Response send(Request.Type type, Object payload) throws Exception {
        Request request = new Request(type, payload);
        return SocketClient.getInstance().sendRequest(request);
    }

    /**
     * Ném Exception nếu server trả về ERROR.
     * Giúp caller chỉ cần try-catch một loại exception.
     */
    private static void requireOk(Response res) throws Exception {
        if (res.isError()) {
            throw new Exception(res.getMessage() != null
                    ? res.getMessage()
                    : "Lỗi không xác định từ server.");
        }
    }
}