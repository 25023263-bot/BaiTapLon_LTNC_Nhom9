package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;

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
     * @param item  vật phẩm đã đóng
     * @param winner null nếu không có bid nào
     */
    void onAuctionClosed(AuctionItem item, Integer winnerId);

    /**
     * Phiên đấu giá chuyển sang ACTIVE (đến giờ mở).
     */
    void onAuctionStarted(AuctionItem item);

    /**
     * Phiên đấu giá bị huỷ bởi người bán.
     */
    void onAuctionCancelled(AuctionItem item);
}
