package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Transaction;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuctionClosedException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.BidTooLowException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Lõi nghiệp vụ đấu giá – điều phối bid, buy-now, close, cancel.
 * Implements Auctionable + Subject trong Observer pattern.
 */
public class AuctionHouse implements Auctionable {

    private static final Logger LOG = Logger.getLogger(AuctionHouse.class.getName());

    private final AuctionRepository     auctionRepo;
    private final BidRepository         bidRepo;
    private final UserRepository        userRepo;
    private final TransactionRepository txRepo;

    private final List<AuctionObserver> observers = new ArrayList<>();

    public AuctionHouse(AuctionRepository auctionRepo, BidRepository bidRepo,
                        UserRepository userRepo, TransactionRepository txRepo) {
        this.auctionRepo = auctionRepo;
        this.bidRepo  = bidRepo;
        this.userRepo = userRepo;
        this.txRepo   = txRepo;
    }

    // ─── Observer Registration ────────────────────────────────────────────────

    public void addObserver(AuctionObserver o)    { observers.add(o); }
    public void removeObserver(AuctionObserver o) { observers.remove(o); }

    private void notifyNewBid(AuctionItem item, Bid bid) {
        observers.forEach(o -> o.onNewBid(item, bid));
    }
    private void notifyClosed(AuctionItem item, Integer winnerId) {
        observers.forEach(o -> o.onAuctionClosed(item, winnerId));
    }
    private void notifyStarted(AuctionItem item) {
        observers.forEach(o -> o.onAuctionStarted(item));
    }
    private void notifyCancelled(AuctionItem item) {
        observers.forEach(o -> o.onAuctionCancelled(item));
    }
    private void notifyExtended(AuctionItem item, LocalDateTime newEndTime) {
        observers.forEach(o -> o.onAuctionExtended(item, newEndTime));
    }

    // ─── Place Bid ────────────────────────────────────────────────────────────

    @Override
    public synchronized Bid placeBid(int itemId, int bidderId, BigDecimal amount)
            throws AuctionClosedException, BidTooLowException, InsufficientBalanceException, Exception {

        AuctionItem item = loadActiveItem(itemId);
        validateBidder(item, bidderId);

        if (!item.isValidBid(amount))
            throw new BidTooLowException(amount, item.getNextMinimumBid());

        Buyer buyer = loadBuyer(bidderId);
        if (!buyer.hasSufficientBalance(amount))
            throw new InsufficientBalanceException(buyer.getWalletBalance(), amount);

        // Tạo và lưu bid
        Bid bid = new Bid(itemId, bidderId, amount);
        bid.setBuyerUsername(buyer.getUsername());
        bidRepo.save(bid);

        // Cập nhật giá hiện tại
        item.updateCurrentBid(amount, bidderId);
        auctionRepo.updateCurrentBid(itemId, amount, bidderId);

        LOG.info(String.format("Bid mới: item #%d, buyer=%s, amount=%,.0f đ",
                itemId, buyer.getUsername(), amount));

        // Kiểm tra gia hạn nếu bid trong phút cuối
        extendIfLastMinute(item);

        notifyNewBid(item, bid);

        // Kích hoạt auto-bid của đối thủ (nếu có)
        triggerAutoBids(item, bidderId);

        return bid;
    }

    // ─── Auto Bid ─────────────────────────────────────────────────────────────

    @Override
    public synchronized Bid placeAutoBid(int itemId, int bidderId, BigDecimal maxLimit)
            throws AuctionClosedException, BidTooLowException, InsufficientBalanceException, Exception {

        AuctionItem item = loadActiveItem(itemId);
        Buyer buyer = loadBuyer(bidderId);

        if (!buyer.hasSufficientBalance(maxLimit))
            throw new InsufficientBalanceException(buyer.getWalletBalance(), maxLimit);

        // Tính mức bid tối thiểu ngay bây giờ
        BigDecimal firstBid = item.getNextMinimumBid();
        if (maxLimit.compareTo(firstBid) < 0)
            throw new BidTooLowException(maxLimit, firstBid);

        // Bid ở mức tối thiểu hiện tại (không bid tối đa ngay)
        BigDecimal bidNow = firstBid;

        Bid bid = new Bid(itemId, bidderId, bidNow);
        bid.setBuyerUsername(buyer.getUsername());
        bid.setAutoBid(true);
        bid.setAutoBidLimit(maxLimit);
        bidRepo.save(bid);

        item.updateCurrentBid(bidNow, bidderId);
        auctionRepo.updateCurrentBid(itemId, bidNow, bidderId);

        LOG.info(String.format("Auto-bid đặt: item #%d, buyer=%s, now=%,.0f, limit=%,.0f",
                itemId, buyer.getUsername(), bidNow, maxLimit));

        notifyNewBid(item, bid);
        return bid;
    }

    /**
     * Sau khi có bid mới, tìm auto-bid của người khác và kích hoạt nếu có.
     */
    private void triggerAutoBids(AuctionItem item, int lastBidderId) throws Exception {
        // Tìm auto-bid cao nhất của người khác
        List<Bid> allBids = bidRepo.findByAuctionId(item.getId());
        for (Bid existing : allBids) {
            if (existing.getBuyerId() == lastBidderId) continue;
            if (!existing.isAutoBid() || existing.getAutoBidLimit() == null) continue;

            BigDecimal needed = item.getNextMinimumBid();
            if (existing.getAutoBidLimit().compareTo(needed) >= 0) {
                Buyer autoBuyer = loadBuyer(existing.getBuyerId());
                if (!autoBuyer.hasSufficientBalance(needed)) continue;

                Bid counter = new Bid(item.getId(), existing.getBuyerId(), needed);
                counter.setBuyerUsername(autoBuyer.getUsername());
                counter.setAutoBid(true);
                counter.setAutoBidLimit(existing.getAutoBidLimit());
                bidRepo.save(counter);

                item.updateCurrentBid(needed, existing.getBuyerId());
                auctionRepo.updateCurrentBid(item.getId(), needed, existing.getBuyerId());

                LOG.info(String.format("Auto-bid counter: buyer=%s, amount=%,.0f",
                        autoBuyer.getUsername(), needed));
                notifyNewBid(item, counter);
                break; // Chỉ counter một lần mỗi lượt
            }
        }
    }

    // ─── Buy Now ──────────────────────────────────────────────────────────────

    @Override
    public synchronized void buyNow(int itemId, int buyerId)
            throws AuctionClosedException, InsufficientBalanceException, Exception {

        AuctionItem item = loadActiveItem(itemId);

        if (!item.hasBuyNow())
            throw new IllegalStateException("Vật phẩm #" + itemId + " không có giá mua ngay.");

        BigDecimal totalCost = item.getBuyNowPrice();
        if (item instanceof PhysicalItem p)
            totalCost = p.getTotalCostForBuyer(); // bao gồm phí ship

        Buyer buyer = loadBuyer(buyerId);
        if (!buyer.hasSufficientBalance(totalCost))
            throw new InsufficientBalanceException(buyer.getWalletBalance(), totalCost);

        // Cập nhật trạng thái item
        item.setCurrentPrice(item.getBuyNowPrice());
        item.setLeadingBidderId(buyerId);
        item.setStatus(AuctionStatus.CLOSED);
        auctionRepo.update(item);

        // Tạo transaction và thanh toán
        processPayment(item, buyerId, totalCost);

        LOG.info(String.format("Buy-Now: item #%d, buyer #%d, price=%,.0f",
                itemId, buyerId, totalCost));
        notifyClosed(item, buyerId);
    }

    // ─── Close Auction ────────────────────────────────────────────────────────

    @Override
    public synchronized void closeAuction(int itemId) throws Exception {
        AuctionItem item = auctionRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy item #" + itemId));

        if (item.getStatus() != AuctionStatus.ACTIVE) {
            LOG.warning("closeAuction gọi trên item không active: #" + itemId);
            return;
        }

        if (!item.hasBids()) {
            // Hết giờ, không có bid → EXPIRED
            item.setStatus(AuctionStatus.EXPIRED);
            auctionRepo.updateStatus(itemId, AuctionStatus.EXPIRED);
            LOG.info("Phiên hết hạn (không có bid): item #" + itemId);
            notifyClosed(item, null);
            return;
        }

        // Có người thắng
        int winnerId = item.getLeadingBidderId();
        item.setStatus(AuctionStatus.CLOSED);
        auctionRepo.updateStatus(itemId, AuctionStatus.CLOSED);

        // Tính tổng tiền buyer phải trả
        BigDecimal totalCost = item.getCurrentPrice();
        if (item instanceof PhysicalItem p)
            totalCost = p.getTotalCostForBuyer();

        processPayment(item, winnerId, totalCost);

        LOG.info(String.format("Phiên kết thúc: item #%d, winner #%d, price=%,.0f",
                itemId, winnerId, item.getCurrentPrice()));
        notifyClosed(item, winnerId);
    }

    // ─── Cancel Auction ───────────────────────────────────────────────────────

    @Override
    public synchronized void cancelAuction(int itemId, int sellerId) throws Exception {
        AuctionItem item = auctionRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy item #" + itemId));

        if (item.getSellerId() != sellerId)
            throw new SecurityException("Không có quyền huỷ phiên đấu giá này.");

        if (item.hasBids())
            throw new IllegalStateException("Không thể huỷ phiên đã có bid.");

        if (item.getStatus() == AuctionStatus.CLOSED || item.getStatus() == AuctionStatus.EXPIRED)
            throw new IllegalStateException("Phiên đã kết thúc, không thể huỷ.");

        item.setStatus(AuctionStatus.CANCELLED);
        auctionRepo.updateStatus(itemId, AuctionStatus.CANCELLED);

        LOG.info("Phiên bị huỷ: item #" + itemId + " bởi seller #" + sellerId);
        notifyCancelled(item);
    }

    // ─── List Item ────────────────────────────────────────────────────────────

    @Override
    public AuctionItem listItem(AuctionItem item) throws Exception {
        if (!item.isValidItem())
            throw new IllegalArgumentException("Thông tin vật phẩm không hợp lệ.");

        if (item.getStartTime().isBefore(LocalDateTime.now().minusMinutes(1)))
            throw new IllegalArgumentException("Thời gian bắt đầu không hợp lệ.");

        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES
                .between(item.getStartTime(), item.getEndTime());
        if (durationMinutes < AppConfig.MIN_AUCTION_DURATION_MINUTES)
            throw new IllegalArgumentException(
                    "Thời gian đấu giá tối thiểu là " + AppConfig.MIN_AUCTION_DURATION_MINUTES + " phút.");

        // Nếu start ngay bây giờ → ACTIVE, ngược lại → PENDING
        if (!item.getStartTime().isAfter(LocalDateTime.now()))
            item.setStatus(AuctionStatus.ACTIVE);
        else
            item.setStatus(AuctionStatus.PENDING);

        auctionRepo.save(item);
        LOG.info("Đăng vật phẩm: #" + item.getId() + " – " + item.getTitle());

        if (item.getStatus() == AuctionStatus.ACTIVE) notifyStarted(item);
        return item;
    }

    // ─── Internal Payment ─────────────────────────────────────────────────────

    /**
     * Trừ tiền buyer, cộng tiền seller, lưu transaction.
     */
    private void processPayment(AuctionItem item, int buyerId, BigDecimal totalCost) throws Exception {
        Buyer  buyer  = loadBuyer(buyerId);
        Seller seller = loadSeller(item.getSellerId());

        BigDecimal shippingFee = BigDecimal.ZERO;
        if (item instanceof PhysicalItem p)
            shippingFee = p.getShippingCost() != null ? p.getShippingCost() : BigDecimal.ZERO;

        // Trừ ví buyer
        buyer.deduct(totalCost);
        userRepo.updateWalletBalance(buyerId, buyer.getWalletBalance());

        // Tạo transaction
        Transaction tx = new Transaction(
                item.getId(), buyerId, item.getSellerId(),
                item.getCurrentPrice(), shippingFee,
                AppConfig.PLATFORM_FEE_RATE, "WALLET");
        txRepo.save(tx);

        // Cộng tiền seller (trừ platform fee)
        seller.receivePayment(tx.getSellerReceives());
        userRepo.updateEarningsBalance(item.getSellerId(), seller.getEarningsBalance());

        // Cập nhật wins cho buyer
        buyer.incrementWins();
        userRepo.update(buyer);

        // Đánh dấu transaction hoàn thành
        tx.markCompleted();
        txRepo.updateStatus(tx.getId(), tx.getPaymentStatus(), tx.getCompletedAt());

        LOG.info(String.format("Thanh toán: buyer #%d trả %,.0f đ, seller #%d nhận %,.0f đ",
                buyerId, totalCost, item.getSellerId(), tx.getSellerReceives()));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuctionItem loadActiveItem(int itemId) throws AuctionClosedException, Exception {
        try {
            AuctionItem item = auctionRepo.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy item #" + itemId));
            if (item.getStatus() != AuctionStatus.ACTIVE)
                throw new AuctionClosedException(itemId, item.getStatus());
            return item;
        } catch (AuctionClosedException e) {
            throw e;
        } catch (SQLException e) {
            throw new Exception("Lỗi DB: " + e.getMessage(), e);
        }
    }

    private Buyer loadBuyer(int buyerId) throws Exception {
        User user = userRepo.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy buyer #" + buyerId));
        if (!(user instanceof Buyer))
            throw new IllegalStateException("User #" + buyerId + " không phải Buyer.");
        return (Buyer) user;
    }

    private Seller loadSeller(int sellerId) throws Exception {
        User user = userRepo.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy seller #" + sellerId));
        if (!(user instanceof Seller))
            throw new IllegalStateException("User #" + sellerId + " không phải Seller.");
        return (Seller) user;
    }

    private void validateBidder(AuctionItem item, int bidderId) throws Exception {
        if (item.getSellerId() == bidderId)
            throw new IllegalStateException("Người bán không thể đặt bid cho chính mình.");
    }

    /**
     * Anti-sniping: nếu có bid mới trong {@code ANTI_SNIPE_WINDOW_SECONDS} giây cuối
     * → gia hạn thêm {@code ANTI_SNIPE_EXTENSION_SECONDS} giây và notify UI.
     *
     * <p>Ví dụ với giá trị mặc định (window=30s, extension=60s):</p>
     * <pre>
     *   Kết thúc dự kiến : 20:00:00
     *   19:59:50 có bid  → còn 10s &lt; window 30s → kéo dài đến 20:01:00
     * </pre>
     *
     * Dùng {@link AuctionRepository#updateEndTime} thay vì {@code update(item)}
     * để chỉ ghi đúng 1 cột, tránh ghi đè dữ liệu không liên quan.
     */
    private void extendIfLastMinute(AuctionItem item) throws SQLException {
        if (item.getRemainingSeconds() < AppConfig.ANTI_SNIPE_WINDOW_SECONDS) {
            LocalDateTime newEnd = item.getEndTime()
                    .plusSeconds(AppConfig.ANTI_SNIPE_EXTENSION_SECONDS);
            item.setEndTime(newEnd);
            auctionRepo.updateEndTime(item.getId(), newEnd);   // ← lightweight: chỉ update end_time
            LOG.info(String.format(
                    "Anti-snipe kích hoạt: item #%d còn %ds → gia hạn +%ds → kết thúc lúc %s",
                    item.getId(), item.getRemainingSeconds(),
                    AppConfig.ANTI_SNIPE_EXTENSION_SECONDS, newEnd));
            notifyExtended(item, newEnd);                       // ← UI timer tự reset
        }
    }
}