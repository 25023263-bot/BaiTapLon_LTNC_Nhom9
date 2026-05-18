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
 *   PLACE_BID, PLACE_AUTO_BID, BUY_NOW,
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
                case BUY_NOW            -> handleBuyNow(req);
                case CANCEL_AUCTION     -> handleCancelAuction(req);
                case DEPOSIT_WALLET     -> handleDepositWallet(req);
                case LOGOUT             -> handleLogout();
                case CREATE_LISTING     -> handleCreateListing(req);
                case UPGRADE_TO_SELLER  -> handleUpgradeToSeller(req);
                case GET_USERS          -> handleGetUsers();
                case TOGGLE_USER_LOCK   -> handleToggleUserLock(req);
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
        var auctions = locator.getAuctionRepo().findAll();
        return Response.ok(auctions);
    }

    /**
     * Trả về ItemDTO chứa đầy đủ thông tin một phiên đấu giá,
     * bao gồm số lượt bid và username của người dẫn đầu.
     */
    private Response handleGetAuctionDetail(Request req) throws Exception {
        Integer auctionId = (Integer) req.getPayload();

        var itemOpt = locator.getAuctionRepo().findById(auctionId);
        if (itemOpt.isEmpty()) {
            return Response.error("Không tìm thấy phiên đấu giá #" + auctionId);
        }

        AuctionItem item = itemOpt.get();

        // Map AuctionItem → ItemDTO để không truyền thẳng domain object
        ItemDTO dto = mapToItemDTO(item);

        // Bổ sung thông tin bid (không có sẵn trong AuctionItem)
        int totalBids = locator.getBidRepo().countByAuctionId(auctionId);
        dto.setTotalBids(totalBids);

        var leadingBid = locator.getBidRepo().findLeadingBid(auctionId);
        leadingBid.ifPresent(bid -> {
            dto.setLeadingBidderId(bid.getBuyerId());
            dto.setLeadingBidderUsername(bid.getBuyerUsername());
        });

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
     * Buy Now: đặt bid bằng chính buyNowPrice để kết thúc phiên ngay lập tức.
     * Payload: BidDTO với amount = buyNowPrice.
     */
    private Response handleBuyNow(Request req) throws Exception {
        BidDTO dto = (BidDTO) req.getPayload();

        var itemOpt = locator.getAuctionRepo().findById(dto.getAuctionId());
        if (itemOpt.isEmpty()) {
            return Response.error("Không tìm thấy phiên đấu giá #" + dto.getAuctionId());
        }
        AuctionItem item = itemOpt.get();

        if (item.getStatus() != AuctionStatus.ACTIVE) {
            return Response.error("Phiên đấu giá đã kết thúc.");
        }
        if (item.getBuyNowPrice() == null || item.getBuyNowPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Response.error("Sản phẩm này không hỗ trợ Mua ngay.");
        }

        // Đặt bid bằng buyNowPrice → AuctionHouse sẽ xử lý đóng phiên
        Bid bid = locator.getAuctionHouse().placeBid(
                dto.getAuctionId(),
                dto.getBuyerId(),
                item.getBuyNowPrice()
        );
        // Đóng phiên ngay sau khi Buy Now thành công
        locator.getAuctionHouse().closeAuction(dto.getAuctionId());

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
        locator.getWalletDepositService().deposit(userOpt.get(), amount);
        LOG.info("Nạp ví thành công: userId=" + dto.getId() + ", amount=" + amount);
        return Response.ok("Nạp " + amount.toPlainString() + " đ thành công.");
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

    // ── Mapper ────────────────────────────────────────────────────────────────

    /**
     * Chuyển AuctionItem domain object sang ItemDTO để gửi qua socket.
     * Tách riêng để dễ mở rộng sau này (thêm trường, xử lý null...).
     */
    private ItemDTO mapToItemDTO(AuctionItem item) {
        ItemDTO dto = new ItemDTO();
        dto.setId(item.getId());
        dto.setSellerId(item.getSellerId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setImageUrl(item.getImageUrl());
        dto.setItemType(item.getClass().getSimpleName()
                .replace("Item", "").toUpperCase()); // PhysicalItem → PHYSICAL
        dto.setStartingPrice(item.getStartingPrice());
        dto.setMinBidIncrement(item.getMinBidIncrement());
        dto.setBuyNowPrice(item.getBuyNowPrice());
        dto.setCurrentPrice(item.getCurrentPrice());
        dto.setStatus(item.getStatus());
        dto.setStartTime(item.getStartTime());
        dto.setEndTime(item.getEndTime());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }
}
