package com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.WatchlistRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Nhận sự kiện từ AuctionHouse và phân phối thông báo tới UI.
 *
 * Trong app desktop (JavaFX, không có push server), notification
 * được lưu in-memory và UI poll hoặc subscribe qua callback.
 */
public class NotificationService implements AuctionObserver {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    /** Hàng đợi thông báo per-user: userId → list messages */
    private final Map<Integer, List<String>> inbox = new ConcurrentHashMap<>();

    /** UI callbacks đăng ký để nhận real-time update */
    private final List<Consumer<NotificationEvent>> uiListeners = new ArrayList<>();

    private final WatchlistRepository watchlistRepo;

    public NotificationService(WatchlistRepository watchlistRepo) {
        this.watchlistRepo = watchlistRepo;
    }

    // ─── AuctionObserver ─────────────────────────────────────────────────────

    @Override
    public void onNewBid(AuctionItem item, Bid bid) {
        String msg = String.format("💰 Bid mới trên \"%s\": %,.0f đ bởi %s",
                item.getTitle(), bid.getAmount(), bid.getBidderUsername());

        // Thông báo cho người dẫn đầu cũ (nếu bị vượt qua)
        // Thông báo cho người theo dõi watchlist
        try {
            List<Integer> watchers = watchlistRepo.findItemIdsByBuyer(item.getId());
            // watchers ở đây là item IDs, cần reverse lookup – simplified:
            push(item.getLeadingBidderId(), msg);
        } catch (Exception e) {
            LOG.warning("Lỗi gửi notification bid: " + e.getMessage());
        }

        broadcast(new NotificationEvent(NotificationEvent.Type.NEW_BID, item, bid, null));
    }

    @Override
    public void onAuctionClosed(AuctionItem item, Integer winnerId) {
        if (winnerId != null) {
            push(winnerId, String.format("🏆 Chúc mừng! Bạn đã thắng đấu giá \"%s\" với giá %,.0f đ",
                    item.getTitle(), item.getCurrentPrice()));
            push(item.getSellerId(), String.format("✅ Phiên \"%s\" đã kết thúc. Người thắng #%d",
                    item.getTitle(), winnerId));
        } else {
            push(item.getSellerId(), String.format("⚠️ Phiên \"%s\" hết hạn mà không có bid nào.",
                    item.getTitle()));
        }
        broadcast(new NotificationEvent(NotificationEvent.Type.AUCTION_CLOSED, item, null, winnerId));
    }

    @Override
    public void onAuctionStarted(AuctionItem item) {
        push(item.getSellerId(), String.format("🔔 Phiên đấu giá \"%s\" đã bắt đầu!", item.getTitle()));
        broadcast(new NotificationEvent(NotificationEvent.Type.AUCTION_STARTED, item, null, null));
    }

    @Override
    public void onAuctionCancelled(AuctionItem item) {
        push(item.getSellerId(), String.format("❌ Phiên \"%s\" đã bị huỷ.", item.getTitle()));
        broadcast(new NotificationEvent(NotificationEvent.Type.AUCTION_CANCELLED, item, null, null));
    }

    // ─── Inbox API ───────────────────────────────────────────────────────────

    /** Lấy tất cả thông báo của user và xoá khỏi hàng đợi. */
    public List<String> drainInbox(int userId) {
        List<String> msgs = inbox.remove(userId);
        return msgs != null ? msgs : List.of();
    }

    /** Số thông báo chưa đọc. */
    public int unreadCount(int userId) {
        List<String> msgs = inbox.get(userId);
        return msgs != null ? msgs.size() : 0;
    }

    // ─── UI Listener ─────────────────────────────────────────────────────────

    /** Controller đăng ký để nhận sự kiện real-time (JavaFX Platform.runLater). */
    public void addUiListener(Consumer<NotificationEvent> listener) {
        uiListeners.add(listener);
    }

    public void removeUiListener(Consumer<NotificationEvent> listener) {
        uiListeners.remove(listener);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void push(int userId, String message) {
        inbox.computeIfAbsent(userId, k -> new ArrayList<>()).add(message);
        LOG.fine("Notification → user #" + userId + ": " + message);
    }

    private void broadcast(NotificationEvent event) {
        uiListeners.forEach(l -> {
            try { l.accept(event); }
            catch (Exception e) { LOG.warning("UI listener lỗi: " + e.getMessage()); }
        });
    }

    // ─── Event DTO ────────────────────────────────────────────────────────────

    public static class NotificationEvent {
        public enum Type { NEW_BID, AUCTION_CLOSED, AUCTION_STARTED, AUCTION_CANCELLED }

        public final Type        type;
        public final AuctionItem item;
        public final Bid         bid;        // null nếu không phải NEW_BID
        public final Integer     winnerId;   // null nếu không phải CLOSED

        public NotificationEvent(Type type, AuctionItem item, Bid bid, Integer winnerId) {
            this.type     = type;
            this.item     = item;
            this.bid      = bid;
            this.winnerId = winnerId;
        }
    }
}