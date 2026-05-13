package com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.NotificationRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.WatchlistRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionObserver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nhận sự kiện từ AuctionHouse và phân phối thông báo đúng người nhận.
 *
 * ── Ai nhận thông báo khi có bid mới? ────────────────────────────────────
 *   1. Seller tạo phiên  → nhận NEW_BID  ("Có bid mới trên phiên của bạn")
 *   2. Buyers đã bid trước đó → nhận OUTBID ("Bạn vừa bị vượt giá")
 *      Tìm từ: SELECT DISTINCT buyer_id FROM bids WHERE auction_id = ?
 *      Loại trừ: người vừa bid (không tự notify mình) + seller (đã notify riêng)
 *
 * ── Persistent vs In-memory ───────────────────────────────────────────────
 *   Phiên bản cũ: lưu in-memory → mất khi restart app.
 *   Phiên bản này: persist vào bảng notifications qua NotificationRepository.
 *   UI vẫn nhận real-time qua uiListeners (callback) — không cần polling.
 */
public class NotificationService implements AuctionObserver {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    // ─── Dependencies ────────────────────────────────────────────────────────
    private final NotificationRepository notifRepo;
    private final BidRepository          bidRepo;
    private final WatchlistRepository    watchlistRepo;

    /** UI callbacks — controller đăng ký để nhận push real-time (same JVM). */
    private final List<Consumer<NotificationEvent>> uiListeners = new ArrayList<>();

    /**
     * Cache unread count per-user: tránh query DB mỗi lần HomeController hỏi.
     * Bị invalidate (remove) mỗi khi có thông báo mới được lưu cho user đó.
     */
    private final ConcurrentHashMap<Integer, Integer> unreadCache = new ConcurrentHashMap<>();

    // ─── Constructor ─────────────────────────────────────────────────────────

    public NotificationService(WatchlistRepository watchlistRepo,
                               BidRepository bidRepo,
                               NotificationRepository notifRepo) {
        this.watchlistRepo = watchlistRepo;
        this.bidRepo       = bidRepo;
        this.notifRepo     = notifRepo;
    }

    // ─── AuctionObserver ─────────────────────────────────────────────────────

    @Override
    public void onNewBid(AuctionItem item, Bid bid) {
        try {
            // 1. Seller nhận NEW_BID
            persist(item.getSellerId(), item.getId(), Notification.Type.NEW_BID,
                    String.format("Bid mới trên \"%s\": %,.0f đ bởi %s",
                            item.getTitle(), bid.getAmount(), bid.getBuyerUsername()));

            // 2. Buyers đã bid trước nhận OUTBID
            //    Dùng bảng bids thay vì watchlist: buyer có thể bid mà không watch
            String msgOutbid = String.format(
                    "Bạn vừa bị vượt giá trên \"%s\" — giá mới: %,.0f đ",
                    item.getTitle(), bid.getAmount());

            Set<Integer> prevBidders = bidRepo.findDistinctBuyerIds(item.getId());
            prevBidders.remove(bid.getBuyerId());    // không tự notify người vừa bid
            prevBidders.remove(item.getSellerId());  // seller đã nhận NEW_BID bên trên

            for (int buyerId : prevBidders) {
                persist(buyerId, item.getId(), Notification.Type.OUTBID, msgOutbid);
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi gửi notification onNewBid", e);
        }

        broadcast(new NotificationEvent(NotificationEvent.Type.NEW_BID, item, bid, null));
    }

    @Override
    public void onAuctionClosed(AuctionItem item, Integer winnerId) {
        try {
            if (winnerId != null) {
                persist(winnerId, item.getId(), Notification.Type.AUCTION_CLOSED,
                        String.format("Chúc mừng! Bạn đã thắng \"%s\" với giá %,.0f đ",
                                item.getTitle(), item.getCurrentPrice()));
                persist(item.getSellerId(), item.getId(), Notification.Type.AUCTION_CLOSED,
                        String.format("Phiên \"%s\" đã kết thúc. Người thắng: #%d  |  Giá: %,.0f đ",
                                item.getTitle(), winnerId, item.getCurrentPrice()));
                // Notify người thua
                Set<Integer> losers = bidRepo.findDistinctBuyerIds(item.getId());
                losers.remove(winnerId);
                losers.remove(item.getSellerId());
                for (int loserId : losers) {
                    persist(loserId, item.getId(), Notification.Type.AUCTION_CLOSED,
                            String.format("Phiên \"%s\" đã kết thúc. Bạn không thắng lần này.",
                                    item.getTitle()));
                }
            } else {
                persist(item.getSellerId(), item.getId(), Notification.Type.AUCTION_CLOSED,
                        String.format("Phiên \"%s\" hết hạn mà không có bid nào.", item.getTitle()));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi gửi notification onAuctionClosed", e);
        }
        broadcast(new NotificationEvent(NotificationEvent.Type.AUCTION_CLOSED, item, null, winnerId));
    }

    @Override
    public void onAuctionStarted(AuctionItem item) {
        try {
            persist(item.getSellerId(), item.getId(), Notification.Type.AUCTION_STARTED,
                    String.format("Phiên đấu giá \"%s\" đã bắt đầu!", item.getTitle()));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi gửi notification onAuctionStarted", e);
        }
        broadcast(new NotificationEvent(NotificationEvent.Type.AUCTION_STARTED, item, null, null));
    }

    @Override
    public void onAuctionCancelled(AuctionItem item) {
        try {
            persist(item.getSellerId(), item.getId(), Notification.Type.AUCTION_CANCELLED,
                    String.format("Phiên \"%s\" đã bị huỷ.", item.getTitle()));
            Set<Integer> bidders = bidRepo.findDistinctBuyerIds(item.getId());
            bidders.remove(item.getSellerId());
            for (int buyerId : bidders) {
                persist(buyerId, item.getId(), Notification.Type.AUCTION_CANCELLED,
                        String.format("Phiên \"%s\" bạn đang tham gia đã bị huỷ bởi người bán.",
                                item.getTitle()));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi gửi notification onAuctionCancelled", e);
        }
        broadcast(new NotificationEvent(NotificationEvent.Type.AUCTION_CANCELLED, item, null, null));
    }

    /** Anti-snipe: phiên vừa được gia hạn → notify buyers đang có bid. */
    @Override
    public void onAuctionExtended(AuctionItem item, LocalDateTime newEndTime) {
        try {
            Set<Integer> bidders = bidRepo.findDistinctBuyerIds(item.getId());
            bidders.remove(item.getSellerId());
            String msg = String.format(
                    "Phiên \"%s\" vừa được gia hạn — kết thúc lúc %s",
                    item.getTitle(),
                    newEndTime.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")));
            for (int buyerId : bidders) {
                persist(buyerId, item.getId(), Notification.Type.ANTI_SNIPE, msg);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi gửi notification onAuctionExtended", e);
        }
    }

    // ─── Public API cho HomeController ───────────────────────────────────────

    /** Lấy danh sách thông báo để render trên notification panel (tối đa 50). */
    public List<Notification> getNotifications(int userId) {
        try {
            return notifRepo.findByUser(userId);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi đọc notifications user #" + userId, e);
            return List.of();
        }
    }

    /**
     * Số thông báo chưa đọc — badge đỏ trên bell icon.
     * Cache-first: chỉ query DB khi cache bị invalidate bởi thông báo mới.
     */
    public int unreadCount(int userId) {
        return unreadCache.computeIfAbsent(userId, id -> {
            try { return notifRepo.countUnread(id); }
            catch (Exception e) { return 0; }
        });
    }

    /** Mở notification panel → đánh dấu tất cả đã đọc → badge về 0. */
    public void markAllRead(int userId) {
        try {
            notifRepo.markAllRead(userId);
            unreadCache.put(userId, 0);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi markAllRead user #" + userId, e);
        }
    }

    /** Click vào 1 thông báo cụ thể → đánh dấu đã đọc. */
    public void markRead(int notificationId, int userId) {
        try {
            notifRepo.markRead(notificationId);
            unreadCache.remove(userId);   // invalidate → query lại lần sau
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi markRead #" + notificationId, e);
        }
    }

    // ─── UI Listener ─────────────────────────────────────────────────────────

    public void addUiListener(Consumer<NotificationEvent> listener) {
        uiListeners.add(listener);
    }

    public void removeUiListener(Consumer<NotificationEvent> listener) {
        uiListeners.remove(listener);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void persist(int userId, Integer auctionId,
                         Notification.Type type, String message) throws Exception {
        notifRepo.save(new Notification(userId, auctionId, type, message));
        unreadCache.remove(userId);   // invalidate cache của người nhận
        LOG.fine("Notification → user #" + userId + " [" + type + "]: " + message);
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
        public final Bid         bid;
        public final Integer     winnerId;

        public NotificationEvent(Type type, AuctionItem item, Bid bid, Integer winnerId) {
            this.type     = type;
            this.item     = item;
            this.bid      = bid;
            this.winnerId = winnerId;
        }
    }
}