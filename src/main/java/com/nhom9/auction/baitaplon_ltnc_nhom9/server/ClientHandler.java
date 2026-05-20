package com.nhom9.auction.baitaplon_ltnc_nhom9.server;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Request;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Response;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing.ListingRequest;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

/**
 * Xử lý tất cả request từ 1 client cụ thể.
 * Mỗi instance chạy trên 1 thread riêng.
 *
 * Đã implement đầy đủ 10 loại request:
 *   LOGIN, REGISTER, GET_AUCTIONS, GET_AUCTION_DETAIL,
 *   PLACE_BID, PLACE_AUTO_BID,
 *   CANCEL_AUCTION, DEPOSIT_WALLET, LOGOUT
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ServiceLocator locator;
    private ObjectOutputStream out; // giữ lại để push notification

    public ClientHandler(Socket socket, ServiceLocator locator) {
        this.socket  = socket;
        this.locator = locator;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())
        ) {
            this.out = out;
            LOG.info("Bắt đầu xử lý client: " + socket.getInetAddress());

            while (true) {
                Request req = (Request) in.readObject();
                LOG.info("Nhận request: " + req);

                Response res = handle(req);

                out.writeObject(res);
                out.flush();
                out.reset(); // tránh ObjectOutputStream cache object cũ
            }

        } catch (EOFException | java.net.SocketException e) {
            LOG.info("Client ngắt kết nối: " + socket.getInetAddress());
        } catch (Exception e) {
            LOG.warning("Lỗi với client " + socket.getInetAddress() + ": " + e.getMessage());
        } finally {
            AuctionServer.connectedClients.remove(this);
        }
    }

    /**
     * Server chủ động push notification về client (không cần client request).
     * Được gọi bởi SocketNotifier khi có sự kiện mới.
     */
    public synchronized void sendNotification(Response notification) {
        try {
            if (out != null && !socket.isClosed()) {
                out.writeObject(notification);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            LOG.warning("Không gửi được notification: " + e.getMessage());
        }
    }

    // ── Điều phối request ────────────────────────────────────────────────────

    private Response handle(Request req) {
        try {
            return switch (req.getType()) {
                case LOGIN              -> handleLogin(req);
                case REGISTER           -> handleRegister(req);
                case GET_AUCTIONS       -> handleGetAuctions();
                case GET_AUCTION_DETAIL -> handleGetAuctionDetail(req);
                case PLACE_BID          -> handlePlaceBid(req);
                case PLACE_AUTO_BID     -> handlePlaceAutoBid(req);
                case CANCEL_AUCTION     -> handleCancelAuction(req);
                case DEPOSIT_WALLET     -> handleDepositWallet(req);
                case LOGOUT             -> handleLogout();
                case CREATE_LISTING     -> handleCreateListing(req);
                case UPGRADE_TO_SELLER  -> handleUpgradeToSeller(req);
                case UPDATE_AUCTION     -> handleUpdateAuction(req);
                case GET_USERS          -> handleGetUsers();
                case TOGGLE_USER_LOCK   -> handleToggleUserLock(req);
                case GET_NOTIFICATIONS          -> handleGetNotifications(req);
                case MARK_NOTIFICATION_READ     -> handleMarkNotificationRead(req);
                case MARK_ALL_NOTIFICATIONS_READ -> handleMarkAllNotificationsRead(req);
                case CLEAR_NOTIFICATIONS        -> handleClearNotifications(req);
            };
        } catch (Exception e) {
            LOG.warning("Lỗi xử lý " + req.getType() + ": " + e.getMessage());
            return Response.error(e.getMessage());
        }
    }

    // ── Các handler ─────────────────────────────────────────────────────────

    private Response handleLogin(Request req) throws Exception {
        UserDTO dto = (UserDTO) req.getPayload();
        // Convention: client gửi password trong field phone của UserDTO
        User user = locator.getAuthService().login(dto.getUsername(), dto.getPhone());

        UserDTO result = new UserDTO();
        result.setId(user.getId());
        result.setUsername(user.getUsername());
        result.setEmail(user.getEmail());
        result.setRole(user.getRole());
        result.setFullName(user.getFullName());
        result.setActive(user.isActive());

        // Quan trọng: gửi kèm số dư ví để client hiển thị ngay sau đăng nhập
        // Không set walletBalance → client nhận null → refreshWallet() hiện 0đ mãi
        if (user instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer buyer) {
            result.setWalletBalance(buyer.getWalletBalance());
        } else if (user instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller seller) {
            result.setEarningsBalance(seller.getEarningsBalance());
        }

        LOG.info("Login thành công: " + user.getUsername());
        return Response.ok(result);
    }

    private Response handleRegister(Request req) throws Exception {
        UserDTO dto = (UserDTO) req.getPayload();
        User user = locator.getAuthService().register(
                dto.getUsername(), dto.getEmail(),
                dto.getPhone(),    // phone = password (convention)
                dto.getFullName(), dto.getPhone(),
                dto.getRole().name()
        );
        return Response.ok("Đăng ký thành công: " + user.getUsername());
    }

    private Response handleGetAuctions() throws Exception {
        List<AuctionItem> auctions = locator.getAuctionRepo().findAll();
        // Chuyển sang ItemDTO (có bidCount thực từ DB) thay vì trả AuctionItem thô.
        // Client dùng AuctionCardMapper.toCardSimple(AuctionItem) → hardcode bidCount=0.
        // Với ItemDTO.totalBids, client dùng toCardFromDTO() → bidCount đúng.
        List<ItemDTO> dtos = auctions.stream()
                .map(this::mapToItemDTO)
                .toList();
        return Response.ok(dtos);
    }

    /**
     * Trả về ItemDTO chứa đầy đủ thông tin một phiên đấu giá,
     * bao gồm số lượt bid, username người dẫn đầu, và danh sách lịch sử bid.
     * Danh sách bids dùng để hiển thị lịch sử đấu giá và biểu đồ giá.
     */
    private Response handleGetAuctionDetail(Request req) throws Exception {
        Integer auctionId = (Integer) req.getPayload();

        var itemOpt = locator.getAuctionRepo().findById(auctionId);
        if (itemOpt.isEmpty()) {
            return Response.error("Không tìm thấy phiên đấu giá #" + auctionId);
        }

        AuctionItem item = itemOpt.get();

        // Map AuctionItem → ItemDTO (bidCount đã được đọc từ DB bên trong)
        ItemDTO dto = mapToItemDTO(item);

        // Bổ sung username người dẫn đầu
        var leadingBid = locator.getBidRepo().findLeadingBid(auctionId);
        leadingBid.ifPresent(bid -> {
            dto.setLeadingBidderId(bid.getBuyerId());
            dto.setLeadingBidderUsername(bid.getBuyerUsername());
        });

        // Lấy toàn bộ lịch sử bid — dùng cho bid history list và price chart
        List<com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid> bids =
                locator.getBidRepo().findByAuctionId(auctionId);

        List<com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO> bidDTOs = bids.stream()
                .map(b -> {
                    com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO bdto =
                            new com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO(
                                    b.getId(), b.getAuctionId(), item.getTitle(),
                                    b.getBuyerId(), b.getBuyerUsername(),
                                    b.getAmount(), b.getBidTime(), b.isAutoBid()
                            );
                    // Đánh dấu bid nào đang dẫn đầu
                    leadingBid.ifPresent(lead -> bdto.setLeading(b.getId() == lead.getId()));
                    return bdto;
                })
                .toList();

        dto.setBids(bidDTOs);

        return Response.ok(dto);
    }

    private Response handlePlaceBid(Request req) throws Exception {
        BidDTO dto = (BidDTO) req.getPayload();
        Bid bid = locator.getAuctionHouse().placeBid(
                dto.getAuctionId(),
                dto.getBuyerId(),
                dto.getAmount()
        );
        return Response.ok(bid);
    }

    private Response handlePlaceAutoBid(Request req) throws Exception {
        BidDTO dto = (BidDTO) req.getPayload();
        Bid bid = locator.getAuctionHouse().placeAutoBid(
                dto.getAuctionId(),
                dto.getBuyerId(),
                dto.getAmount()
        );
        return Response.ok(bid);
    }

    /**
     * Cancel Auction: chỉ Seller của phiên hoặc Admin mới được hủy.
     * Payload: Integer (auctionId).
     *          Hoặc int[]{auctionId, requestingUserId} nếu cần kiểm tra quyền.
     */
    /**
     * Payload có thể là:
     *   - Integer  → auctionId (Admin force-close, không kiểm tra sellerId)
     *   - int[]    → {auctionId, sellerId} (Seller hủy phiên của mình)
     */
    private Response handleCancelAuction(Request req) throws Exception {
        int auctionId;
        int sellerId = -1; // -1 = Admin mode, bỏ qua kiểm tra quyền

        Object payload = req.getPayload();
        if (payload instanceof int[] arr && arr.length >= 2) {
            auctionId = arr[0];
            sellerId  = arr[1];
        } else {
            auctionId = (Integer) payload;
        }

        var itemOpt = locator.getAuctionRepo().findById(auctionId);
        if (itemOpt.isEmpty()) {
            return Response.error("Không tìm thấy phiên đấu giá #" + auctionId);
        }
        AuctionItem item = itemOpt.get();
        if (item.getStatus() != AuctionStatus.ACTIVE && item.getStatus() != AuctionStatus.PENDING) {
            return Response.error("Chỉ có thể hủy phiên đang chạy hoặc chờ duyệt.");
        }

        if (sellerId == -1) {
            // Admin: dùng closeAuction (không kiểm tra sellerId)
            locator.getAuctionHouse().closeAuction(auctionId);
        } else {
            // Seller: dùng cancelAuction (kiểm tra sellerId + hasBids)
            locator.getAuctionHouse().cancelAuction(auctionId, sellerId);
        }
        LOG.info("Đã hủy phiên đấu giá #" + auctionId);
        return Response.ok("Phiên #" + auctionId + " đã được hủy.");
    }

    /**
     * Deposit Wallet: nạp tiền vào ví người dùng.
     * Payload: UserDTO với id = userId và phone = số tiền nạp (dạng String).
     */
    private Response handleDepositWallet(Request req) throws Exception {
        UserDTO dto = (UserDTO) req.getPayload();
        // Convention: amount gửi trong field phone của UserDTO
        BigDecimal amount;
        try {
            amount = new BigDecimal(dto.getPhone());
        } catch (NumberFormatException e) {
            return Response.error("Số tiền không hợp lệ: " + dto.getPhone());
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.error("Số tiền nạp phải lớn hơn 0.");
        }

        // WalletDepositService.deposit() cần User object, không phải userId
        var userOpt = locator.getUserRepo().findById(dto.getId());
        if (userOpt.isEmpty()) {
            return Response.error("Không tìm thấy người dùng #" + dto.getId());
        }
        var user = userOpt.get();
        locator.getWalletDepositService().deposit(user, amount);
        LOG.info("Nạp ví thành công: userId=" + dto.getId() + ", amount=" + amount);

        // FIX: Trả về UserDTO chứa số dư MỚI sau khi nạp, thay vì chỉ trả chuỗi.
        // Client dùng số dư này để cập nhật UI ngay lập tức — không cần tự tính toán.
        UserDTO result = new UserDTO();
        result.setId(user.getId());
        if (user instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer buyer) {
            result.setWalletBalance(buyer.getWalletBalance());
        } else if (user instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller seller) {
            result.setEarningsBalance(seller.getEarningsBalance());
        }
        return Response.ok(result);
    }

    /**
     * Logout: server stateless — client xóa session phía mình.
     * Server chỉ log và trả OK.
     */
    private Response handleLogout() {
        LOG.info("Client đăng xuất: " + socket.getInetAddress());
        return Response.ok("Đã đăng xuất.");
    }

    /**
     * Seller đăng bán sản phẩm mới.
     * Payload: ListingRequest (implements Serializable).
     */
    private Response handleCreateListing(Request req) throws Exception {
        ListingRequest listing = (ListingRequest) req.getPayload();
        locator.getListingService().createListing(listing);
        LOG.info("Đăng bán thành công: \"" + listing.title() + "\" bởi sellerId=" + listing.sellerId());
        return Response.ok("Đăng bán thành công.");
    }

    /**
     * Nâng cấp Buyer lên Seller sau khi đồng ý điều khoản.
     * Payload: Integer (userId).
     */
    private Response handleUpgradeToSeller(Request req) throws Exception {
        Integer userId = (Integer) req.getPayload();
        var userOpt = locator.getUserRepo().findById(userId);
        if (userOpt.isEmpty()) {
            return Response.error("Không tìm thấy người dùng #" + userId);
        }
        locator.getUserRepo().upgradeToSeller(userId);
        LOG.info("Nâng cấp Seller thành công: userId=" + userId);
        return Response.ok("Tài khoản đã được nâng cấp thành Người bán.");
    }

    /**
     * Admin lấy toàn bộ danh sách người dùng.
     * Payload: null.
     */
    private Response handleGetUsers() throws Exception {
        List<User> users = locator.getUserRepo().findAll();
        return Response.ok(users);
    }

    /**
     * Admin khoá / mở khoá tài khoản người dùng (toggle).
     * Payload: Integer (userId).
     * Server toggle trạng thái active rồi lưu DB, trả về thông báo kết quả.
     */
    private Response handleToggleUserLock(Request req) throws Exception {
        Integer userId = (Integer) req.getPayload();
        var userOpt = locator.getUserRepo().findById(userId);
        if (userOpt.isEmpty()) {
            return Response.error("Không tìm thấy người dùng #" + userId);
        }
        User user = userOpt.get();
        user.setActive(!user.isActive()); // toggle
        locator.getUserRepo().update(user);
        String action = user.isActive() ? "Đã mở khoá" : "Đã khoá";
        LOG.info(action + " tài khoản: " + user.getUsername());
        return Response.ok(action + " tài khoản " + user.getUsername() + ".");
    }

    /**
     * Seller cập nhật thông tin phiên đấu giá (title, description, endTime, imageUrl).
     * Payload: ItemDTO với id, title, description, endTime, imageUrl được set.
     * Chỉ Seller sở hữu phiên mới được phép cập nhật.
     */
    private Response handleUpdateAuction(Request req) throws Exception {
        ItemDTO dto = (ItemDTO) req.getPayload();
        int auctionId = dto.getId();

        var itemOpt = locator.getAuctionRepo().findById(auctionId);
        if (itemOpt.isEmpty()) {
            return Response.error("Không tìm thấy phiên đấu giá #" + auctionId);
        }
        AuctionItem item = itemOpt.get();

        // Cập nhật các trường được phép sửa
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        if (dto.getEndTime() != null) {
            item.setEndTime(dto.getEndTime());
        }
        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            item.setImageUrl(dto.getImageUrl());
        }

        locator.getAuctionRepo().update(item);
        LOG.info("Cập nhật phiên #" + auctionId + ": \"" + item.getTitle() + "\"");
        return Response.ok("Đã cập nhật sản phẩm.");
    }

    /**
     * Lấy danh sách thông báo của một user.
     * Payload: Integer (userId).
     */
    private Response handleGetNotifications(Request req) throws Exception {
        Integer userId = (Integer) req.getPayload();
        List<Notification> notifications = locator.getNotificationService().getNotifications(userId);
        return Response.ok(notifications);
    }

    /**
     * Đánh dấu một thông báo là đã đọc.
     * Payload: int[] {notificationId, userId}.
     */
    private Response handleMarkNotificationRead(Request req) throws Exception {
        int[] ids = (int[]) req.getPayload();
        locator.getNotificationService().markRead(ids[0], ids[1]);
        return Response.ok("Đã đánh dấu đã đọc.");
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     * Payload: Integer (userId).
     */
    private Response handleMarkAllNotificationsRead(Request req) throws Exception {
        Integer userId = (Integer) req.getPayload();
        locator.getNotificationService().markAllRead(userId);
        return Response.ok("Đã đánh dấu tất cả đã đọc.");
    }

    /**
     * Xóa tất cả thông báo của user.
     * Payload: Integer (userId).
     */
    private Response handleClearNotifications(Request req) throws Exception {
        Integer userId = (Integer) req.getPayload();
        locator.getNotificationService().clearAll(userId);
        return Response.ok("Đã xóa tất cả thông báo.");
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    /**
     * Chuyển AuctionItem domain object sang ItemDTO để gửi qua socket.
     *
     * <p>bidCount được đọc trực tiếp từ bảng bids (COUNT) thay vì lấy từ
     * AuctionItem domain object — vì AuctionItem không tự cập nhật counter này
     * sau mỗi bid. Nếu gọi item.getBidCount() sẽ luôn trả 0.
     */
    private ItemDTO mapToItemDTO(AuctionItem item) {
        ItemDTO dto = new ItemDTO();
        dto.setId(item.getId());
        dto.setSellerId(item.getSellerId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setImageUrl(item.getImageUrl());
        dto.setItemType("PHYSICAL");
        dto.setStartingPrice(item.getStartingPrice());
        dto.setMinBidIncrement(item.getMinBidIncrement());
        dto.setCurrentPrice(item.getCurrentPrice());
        dto.setStatus(item.getStatus());
        dto.setStartTime(item.getStartTime());
        dto.setEndTime(item.getEndTime());
        dto.setCreatedAt(item.getCreatedAt());

        // Đọc bidCount thực từ DB — item domain object không tự track counter này
        try {
            dto.setTotalBids(locator.getBidRepo().countByAuctionId(item.getId()));
        } catch (Exception e) {
            LOG.warning("Không đọc được bidCount cho item #" + item.getId() + ": " + e.getMessage());
            dto.setTotalBids(0);
        }

        return dto;
    }
}