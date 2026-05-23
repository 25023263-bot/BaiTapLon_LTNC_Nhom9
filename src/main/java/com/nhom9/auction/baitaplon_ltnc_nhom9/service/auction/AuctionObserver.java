package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import java.time.LocalDateTime;
/**
 * Observer nhận sự kiện từ AuctionHouse.
 * Implement bởi NotificationService, UI controllers muốn refresh real-time.
 *
 * Pattern: AuctionHouse (Subject) → notifies → List<AuctionObserver>
 */
public interface AuctionObserver {

    /**
     * Có bid mới được đặt thành công.
     * @param item trạng thái vật phẩm sau khi update
     * @param bid  bid vừa được chấp nhận
     */
    void onNewBid(AuctionItem item, Bid bid);

    /**
     * Phiên đấu giá kết thúc (có người thắng hoặc hết hạn).
     *
     * event chứa:
     *   - item           : vật phẩm đã đóng
     *   - winnerId       : null nếu không có bid nào
     *   - buyerNewBalance: số dư mới của buyer sau khi bị trừ tiền (null nếu không có winner)
     *   - sellerId       : ID của seller
     *   - sellerNewBalance: số earnings mới của seller sau khi được cộng tiền
     *
     * Client dùng sellerId/winnerId để biết mình có liên quan không,
     * rồi cập nhật số dư trong UserSession ngay lập tức.
     */
    void onAuctionClosed(AuctionClosedEvent event);

    /**
     * Phiên đấu giá chuyển sang ACTIVE (đến giờ mở).
     */
    void onAuctionStarted(AuctionItem item);

    /**
     * Phiên đấu giá bị huỷ bởi người bán.
     */
    void onAuctionCancelled(AuctionItem item);

    /**
     * Anti-snipe kích hoạt: phiên vừa được gia hạn do có bid trong cửa sổ cuối.
     *
     * <p>Default method → các class đã implement interface này KHÔNG bị lỗi
     * compile. Chỉ những observer thực sự cần xử lý gia hạn (ví dụ UI timer)
     * mới cần override.</p>
     *
     * @param item       vật phẩm vừa được gia hạn
     * @param newEndTime thời điểm kết thúc mới (sau gia hạn)
     */
    default void onAuctionExtended(AuctionItem item, LocalDateTime newEndTime) {
        // no-op — override khi cần cập nhật UI timer
    }
}